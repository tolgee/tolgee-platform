package io.tolgee.unit.formats.json.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test
import java.io.InputStream

class JsonFileExporterTest {
  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it scopes and handles collisions`() {
    val data = generateTranslationsForKeys(listOf("a.a.a.a", "a.a", "a.a.a", "a.b.b", "a.c.c", "b", "b.b"))
    val exported = getExporter(data).produceFiles()
    val json = exported.getFileTextContent("en.json")
    val parsed =
      jacksonObjectMapper()
        .readValue<Map<String, Any>>(json)

    val a = (parsed["a"] as Map<String, String>)
    val aa = a["a"]
    val aaa = a["a.a"]
    val aaaa = a["a.a.a"]
    val b = parsed["b"]
    val bb = parsed["b.b"]

    listOf(aa, aaa, aaaa, b, bb).forEach {
      assertThat(it).isEqualTo("text")
    }
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it exports when key starts with dot`() {
    val data = generateTranslationsForKeys(listOf(".a"))
    val exported = getExporter(data).produceFiles()
    val json = exported.getFileTextContent("en.json")
    val parsed =
      jacksonObjectMapper()
        .readValue<Map<String, Any>>(json)

    val map = (parsed[""] as Map<String, String>)
    val a = map["a"]
    assertThat(a).isEqualTo("text")
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it scopes by namespaces`() {
    val data = generateTranslationsForKeys(listOf("a:a.a", "a", "a:a", "a:b.a"))
    val exported = getExporter(data).produceFiles()

    val ajson = exported.getFileTextContent("en.json")
    assertThatJson(ajson) {
      node("a").isEqualTo("text")
    }
    val aajson = exported.getFileTextContent("a/en.json")
    assertThatJson(aajson) {
      node("a").isEqualTo("text")
      node("a\\.a").isEqualTo("text")
      node("b.a").isEqualTo("text")
    }
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it returns result in the same order as it comes from DB`() {
    val keys = listOf("a", "b", "c", "d", "e", "f")
    val data = generateTranslationsForKeys(keys)
    val exported = getExporter(data).produceFiles()
    val parsed: LinkedHashMap<String, String> = exported.parseFileContent("en.json")
    assertThat(parsed.keys.toList()).isEqualTo(keys)
  }

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it is formatted`() {
    val keys = listOf("a", "b")
    val data = generateTranslationsForKeys(keys)
    val exported = getExporter(data).produceFiles()
    assertThat(exported.getFileTextContent("en.json")).contains("\n").contains("  ")
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "key3" : "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}",
    |  "item" : "I will be first {icuParam, number}"
    |}
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): JsonFileExporter {
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
      "cs.json",
      """
    |{
    |  "key3" : "{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}",
    |  "item" : "I will be first '{'icuParam'}' {hello, number}"
    |}
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): JsonFileExporter {
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
  ): JsonFileExporter {
    return JsonFileExporter(
      translations = translations,
      exportParams = ExportParams(),
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
      objectMapper = jacksonObjectMapper(),
    )
  }
}
