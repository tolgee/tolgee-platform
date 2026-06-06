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
    // literal string -> not smart
    en.assert.contains("m_Localized: \"Hello\"")
    // placeholder string -> smart
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
    val data = export(simpleTranslations())
    val en = data["Localization_en.asset"]!!
    // the literal entry must be non-smart, the placeholder entry smart
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
    val data = export(built.translations)
    val cs = data["Localization_cs.asset"]!!
    // czech has 4 categories (one, few, many, other) -> 3 pipe separators
    val plural = Regex("m_Localized: \"(\\{count:plural:[^\"]*})\"").find(cs)!!.groupValues[1]
    plural.assert.startsWith("{count:plural:")
    plural.count { it == '|' }.assert.isEqualTo(3)
    cs.assert.contains("m_IsSmart: 1")
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

    val first = export(forward.translations)
    val second = export(reversed.translations)
    first.assert.isEqualTo(second)
  }

  @Test
  fun `preserved unity id and guid take precedence over derived`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "greeting", text = "Hello") {
          key.custom =
            mapOf(
              "_unity" to
                mapOf(
                  "keyId" to 42L,
                  "sharedTableDataGuid" to "1234567890abcdef1234567890abcdef",
                ),
            )
        }
      }
    val data = export(built.translations)
    data["Localization Shared Data.asset"]!!.assert.contains("m_Id: 42")
    data["Localization Shared Data.asset.meta"]!!.assert.contains("guid: 1234567890abcdef1234567890abcdef")
  }

  @Test
  fun `rejects a template containing a language tag placeholder`() {
    val params =
      ExportParams().also {
        it.format = ExportFormat.UNITY
        it.fileStructureTemplate = "{namespace}/{languageTag}"
      }
    val translations =
      buildExportTranslationList { add(languageTag = "en", keyName = "a", text = "A") }.translations
    assertThrows<BadRequestException> {
      ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate()
    }
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
