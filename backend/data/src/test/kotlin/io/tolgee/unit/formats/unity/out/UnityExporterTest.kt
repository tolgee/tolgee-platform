package io.tolgee.unit.formats.unity.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.unity.UnityIdentity
import io.tolgee.formats.unity.out.UnityExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnityExporterTest {
  @Test
  fun `exports shared data and per-locale string tables`() {
    val data = export(simpleTranslations())

    val shared = data["Localization Shared Data.asset"]!!
    shared.assert.contains("m_TableCollectionName: \"Localization\"")
    shared.assert.contains("m_Key: \"greeting\"")
    shared.assert.contains("m_Key: \"welcome\"")

    val en = data["Localization_en.asset"]!!
    en.assert.contains("m_Code: \"en\"")
    en.assert.contains("m_SharedData: {fileID: 11400000")
    en.assert.contains("m_Localized: \"Hello\"")
    en.assert.contains("m_Localized: \"Hi {name}\"")
  }

  @Test
  fun `links string table entries to shared data by key id`() {
    val data = export(simpleTranslations())
    val greetingId = UnityIdentity.deriveKeyId(null, "greeting")

    data["Localization Shared Data.asset"]!!.assert.contains("m_Id: $greetingId")
    data["Localization_en.asset"]!!.assert.contains("m_Id: $greetingId")
  }

  @Test
  fun `marks smart vs non-smart entries`() {
    val en = export(simpleTranslations())["Localization_en.asset"]!!
    en.assert.contains("m_Localized: \"Hello\"\n    m_Metadata:\n      m_Items: []\n    m_IsSmart: 0")
    en.assert.contains("m_Localized: \"Hi {name}\"\n    m_Metadata:\n      m_Items: []\n    m_IsSmart: 1")
  }

  @Test
  fun `renders plurals as positional smart format pipes`() {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "apples",
          text = "{count, plural, one {# apple} few {# apples} other {# apples}}",
        ) {
          key.isPlural = true
        }
      }
    val cs = export(built.translations)["Localization_cs.asset"]!!
    val plural = Regex("m_Localized: \"(\\{count:plural:[^\"]*})\"").find(cs)!!.groupValues[1]
    plural.assert.startsWith("{count:plural:")
    plural.count { it == '|' }.assert.isEqualTo(3)
    cs.assert.contains("m_IsSmart: 1")
  }

  @Test
  fun `escapes a literal pipe inside a plural form`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "k", text = "{count, plural, one {a|b} other {c}}") {
          key.isPlural = true
        }
      }
    val en = export(built.translations)["Localization_en.asset"]!!
    // the literal pipe is backslash-escaped (then YAML-escaped), structural pipe stays bare
    en.assert.contains("a\\\\|b|c")
  }

  @Test
  fun `is deterministic regardless of input ordering`() {
    val forward =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "a", text = "A")
        add(languageTag = "en", keyName = "b", text = "B")
        add(languageTag = "cs", keyName = "a", text = "Á")
      }
    val reversed =
      buildExportTranslationList {
        add(languageTag = "cs", keyName = "a", text = "Á")
        add(languageTag = "en", keyName = "b", text = "B")
        add(languageTag = "en", keyName = "a", text = "A")
      }

    export(forward.translations).assert.isEqualTo(export(reversed.translations))
  }

  @Test
  fun `key-level smart flag covers all locales and missing translations`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "m", text = "Hi {name}")
        add(languageTag = "cs", keyName = "m", text = "Ahoj")
        add(languageTag = "en", keyName = "only-en", text = "x")
      }
    val data = export(built.translations)
    val cs = data["Localization_cs.asset"]!!
    // smart-ness propagates to cs even though its own text is a plain literal
    cs.assert.contains("m_Localized: \"Ahoj\"\n    m_Metadata:\n      m_Items: []\n    m_IsSmart: 1")
    // a key untranslated in cs still gets a row, emitted empty
    val onlyEnId = UnityIdentity.deriveKeyId(null, "only-en")
    cs.assert.contains("m_Id: $onlyEnId\n    m_Localized: \"\"")
  }

  @Test
  fun `escapes special characters in values`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "k", text = "He said \"hi\"\nbye")
      }
    val en = export(built.translations)["Localization_en.asset"]!!
    en.assert.contains("m_Localized: \"He said \\\"hi\\\"\\nbye\"")
  }

  @Test
  fun `exports one collection per namespace`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "a", text = "A") { key.namespace = "ui" }
        add(languageTag = "en", keyName = "b", text = "B") { key.namespace = "menu" }
      }
    val data = export(built.translations)
    data.keys.assert.contains("ui/ui Shared Data.asset")
    data.keys.assert.contains("menu/menu Shared Data.asset")
    data["ui/ui Shared Data.asset"]!!.assert.contains("m_Key: \"a\"")
    data["menu/menu Shared Data.asset"]!!.assert.contains("m_Key: \"b\"")
    data["ui/ui_en.asset"]!!.assert.contains("m_Localized: \"A\"")
  }

  @Test
  fun `preserved unity id and guid take precedence over derived`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "greeting", text = "Hello") {
          key.custom =
            mapOf(
              "_unityKeyId" to 42L,
              "_unitySharedTableDataGuid" to "1234567890abcdef1234567890abcdef",
            )
        }
      }
    val data = export(built.translations)
    data["Localization Shared Data.asset"]!!.assert.contains("m_Id: 42")
    data["Localization Shared Data.asset.meta"]!!.assert.contains("guid: 1234567890abcdef1234567890abcdef")
  }

  @Test
  fun `preserved isSmart overrides derived smartness`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "literal", text = "plain") { key.custom = mapOf("_unityIsSmart" to true) }
        add(languageTag = "en", keyName = "ph", text = "Hi {name}") { key.custom = mapOf("_unityIsSmart" to false) }
      }
    val en = export(built.translations)["Localization_en.asset"]!!
    en.assert.contains("m_Localized: \"plain\"\n    m_Metadata:\n      m_Items: []\n    m_IsSmart: 1")
    en.assert.contains("m_Localized: \"Hi {name}\"\n    m_Metadata:\n      m_Items: []\n    m_IsSmart: 0")
  }

  @Test
  fun `throws on key id collision`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "a", text = "A") { key.custom = mapOf("_unityKeyId" to 42L) }
        add(languageTag = "en", keyName = "b", text = "B") { key.custom = mapOf("_unityKeyId" to 42L) }
      }
    assertThrows<BadRequestException> { export(built.translations) }
  }

  @Test
  fun `throws on shared table data guid conflict`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "a", text = "A") {
          key.custom = mapOf("_unitySharedTableDataGuid" to "1111111111111111aaaaaaaaaaaaaaaa")
        }
        add(languageTag = "en", keyName = "b", text = "B") {
          key.custom = mapOf("_unitySharedTableDataGuid" to "2222222222222222bbbbbbbbbbbbbbbb")
        }
      }
    assertThrows<BadRequestException> { export(built.translations) }
  }

  @Test
  fun `does not throw when keys share the same preserved guid`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "a", text = "A") {
          key.custom = mapOf("_unitySharedTableDataGuid" to "1111111111111111aaaaaaaaaaaaaaaa")
        }
        add(languageTag = "en", keyName = "b", text = "B") {
          key.custom = mapOf("_unitySharedTableDataGuid" to "1111111111111111aaaaaaaaaaaaaaaa")
        }
      }
    export(built.translations)["Localization Shared Data.asset.meta"]!!
      .assert
      .contains("guid: 1111111111111111aaaaaaaaaaaaaaaa")
  }

  @Test
  fun `rejects a template containing a language tag placeholder`() {
    assertThrows<BadRequestException> { templateProvider("{namespace}/{languageTag}") }
  }

  @Test
  fun `rejects a template containing an extension placeholder`() {
    assertThrows<BadRequestException> { templateProvider("{namespace}/Strings.{extension}") }
  }

  private fun templateProvider(template: String) {
    val params =
      ExportParams().also {
        it.format = ExportFormat.UNITY
        it.fileStructureTemplate = template
      }
    val translations =
      buildExportTranslationList { add(languageTag = "en", keyName = "a", text = "A") }.translations
    ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate()
  }

  private fun simpleTranslations(): List<ExportTranslationView> {
    return buildExportTranslationList {
      add(languageTag = "en", keyName = "greeting", text = "Hello", description = "a greeting")
      add(languageTag = "en", keyName = "welcome", text = "Hi {name}")
    }.translations
  }

  private fun export(translations: List<ExportTranslationView>): Map<String, String> {
    val params = ExportParams().also { it.format = ExportFormat.UNITY }
    val exporter =
      UnityExporter(
        translations = translations,
        exportParams = params,
        isProjectIcuPlaceholdersEnabled = true,
        filePathProvider =
          ExportFilePathProvider(
            template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
            extension = params.format.extension,
          ),
      )
    return exporter.produceFiles().map { it.key to it.value.bufferedReader().readText() }.toMap()
  }
}
