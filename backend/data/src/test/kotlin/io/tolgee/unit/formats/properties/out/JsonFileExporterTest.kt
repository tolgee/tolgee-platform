package io.tolgee.unit.formats.properties.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.properties.out.PropertiesFileExporter
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import java.io.InputStream

class PropertiesFileExporterTest {
  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it exports`() {
    val exporter = getBasicExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.properties",
      """
    |# I am a description
    |key = {value, plural, other {I am basic text}}
    |key.with.dots = I am key with dots
    |escaping\ test = I am key with dots = a = \n # not a comment \n = = \\ yep +áěááššá
    |
      """.trimMargin(),
    )
  }

  private fun getBasicExporter(): PropertiesFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key",
          text = "I am basic text",
        ) {
          key.description = "I am a description"
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "key.with.dots",
          text = "I am key with dots",
        )
        add(
          languageTag = "cs",
          keyName = "escaping test",
          text = "I am key with dots = a = \n # not a comment \n = = \\ yep +áěááššá",
        )
      }
    return getExporter(built.translations, false)
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.properties",
      """
    |key3 = {count, plural, one {# den {icuParam}} few {# dny} other {# dní}}
    |item = I will be first {icuParam, number}
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): PropertiesFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {'#' den '{'icuParam'}'} few {'#' dny} other {'#' dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first {icuParam, number}",
        )
      }
    return getExporter(built.translations, false)
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.properties",
      """
    |key3 = {count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}
    |item = I will be first '{'icuParam'}' {hello, number}
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): PropertiesFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {hello, number}",
        )
      }
    return getExporter(built.translations, true)
  }

  private fun Map<String, InputStream>.getFileTextContent(fileName: String): String {
    return this[fileName]!!.bufferedReader().readText()
  }

  private inline fun <reified T> Map<String, InputStream>.parseFileContent(fileName: String): T {
    return jacksonObjectMapper().readValue(this.getFileTextContent(fileName))
  }

  private fun generateTranslationsForKeys(keys: List<String>): List<ExportTranslationView> {
    return keys.sorted().map { keyDef ->
      val split = keyDef.split(":").toMutableList()
      val keyName = split.removeLast()
      val namespace = split.removeLastOrNull()
      val key = ExportKeyView(1, keyName, namespace = namespace)
      val trans = ExportTranslationView(1, "text", TranslationState.TRANSLATED, key, "en")
      key.translations["en"] = trans
      trans
    }
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
  ): PropertiesFileExporter {
    return PropertiesFileExporter(
      translations = translations,
      exportParams = ExportParams(),
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
    )
  }
}
