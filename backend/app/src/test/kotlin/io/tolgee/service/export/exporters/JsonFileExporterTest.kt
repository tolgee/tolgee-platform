package io.tolgee.service.export.exporters

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.model.enums.TranslationState
import io.tolgee.service.export.dataProvider.ExportKeyView
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assertions.Assertions.assertThat
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.Test

class JsonFileExporterTest {

  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it scopes and handles collisions`() {
    val data = getStructuredTranslations(listOf("a.a.a.a", "a.a", "a.a.a", "a.b.b", "a.c.c", "b", "b.b"))
    val exported = JsonFileExporter(data, ExportParams()).produceFiles()
    val json = exported["/en.json"]!!.bufferedReader().readText()
    val parsed = jacksonObjectMapper()
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
  fun `it scopes to files`() {
    val data = getStructuredTranslations(listOf("a.a.a", "a", "a.a"))
    val exported = JsonFileExporter(
      data,
      ExportParams().apply {
        splitByScope = true
        splitByScopeDepth = 2
      }
    ).produceFiles()

    val ajson = exported["/en.json"]!!.bufferedReader().readText()
    assertThatJson(ajson) {
      node("a").isEqualTo("text")
    }
    val aajson = exported["a/en.json"]!!.bufferedReader().readText()
    assertThatJson(aajson) {
      node("a").isEqualTo("text")
    }
    val aaajson = exported["a/a/en.json"]!!.bufferedReader().readText()
    assertThatJson(aaajson) {
      node("a").isEqualTo("text")
    }
  }

  private fun getStructuredTranslations(keys: List<String>): List<ExportTranslationView> {
    return keys.sorted().map { keyName ->
      val key = ExportKeyView(1, keyName)
      val trans = ExportTranslationView(1, "text", TranslationState.TRANSLATED, key, "en")
      key.translations["en"] = trans
      trans
    }
  }
}
