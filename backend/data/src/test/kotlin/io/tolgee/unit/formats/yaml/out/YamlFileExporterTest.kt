package io.tolgee.unit.formats.yaml.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.unit.formats.yaml.out.YamlExportTestData.getAllFeaturesExporter
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import org.junit.jupiter.api.Test
import java.io.InputStream

class YamlFileExporterTest {
  // generate this with:
  // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
  @Test
  fun `respects support arrays prop`() {
    var data = exportWithParams(supportArrays = true)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |this:
    |  is:
    |    nested:
    |      plural: "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}"
    |  '[1]':
    |    is:
    |      collision: "Colission"
    |this-is-array:
    |- object: "I will be first {icuParam, number}"
    |indexed params: "I will be first {0}, {1}"
    |named params: "I will be first {param1}, {param2}"
    |
      """.trimMargin(),
    )

    data = exportWithParams(supportArrays = false)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |this:
    |  is:
    |    nested:
    |      plural: "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}"
    |this[1]:
    |  is:
    |    collision: "Colission"
    |this-is-array[1]:
    |  object: "I will be first {icuParam, number}"
    |indexed params: "I will be first {0}, {1}"
    |named params: "I will be first {param1}, {param2}"
    |
      """.trimMargin(),
    )
  }

  // generate this with:
  // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
  @Test
  fun `respects message format prop`() {
    val data = exportWithParams(messageFormat = ExportMessageFormat.RUBY_SPRINTF)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |this:
    |  is:
    |    nested:
    |      plural:
    |        one: "%(count)d den %{icuParam}"
    |        few: "%(count)d dny"
    |        other: "%(count)d dní"
    |this[1]:
    |  is:
    |    collision: "Colission"
    |this-is-array[1]:
    |  object: "I will be first %<icuParam>d"
    |indexed params: "I will be first %s, %s"
    |named params: "I will be first %{param1}, %{param2}"
    |
      """.trimMargin(),
    )
  }

  // generate this with:
  // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
  @Test
  fun `respects structure delimiter prop`() {
    val data = exportWithParams(structureDelimiter = null)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |this.is.nested.plural: "{count, plural, one {# den {icuParam}} few {# dny} other {#\
    |  \ dní}}"
    |this[1].is.collision: "Colission"
    |this-is-array[1].object: "I will be first {icuParam, number}"
    |indexed params: "I will be first {0}, {1}"
    |named params: "I will be first {param1}, {param2}"
    |
      """.trimMargin(),
    )
  }

  private fun exportWithParams(
    supportArrays: Boolean = false,
    messageFormat: ExportMessageFormat? = null,
    structureDelimiter: Char? = '.',
  ): Map<String, String> {
    val exporter =
      getAllFeaturesExporter(
        ExportParams().also {
          it.supportArrays = supportArrays
          it.structureDelimiter = structureDelimiter
          it.format = ExportFormat.YAML
          it.messageFormat = messageFormat
        },
      )
    val data = getExported(exporter)
    return data
  }

  private fun Map<String, InputStream>.getFileTextContent(fileName: String): String {
    return this[fileName]!!.bufferedReader().readText()
  }

  private inline fun <reified T> Map<String, InputStream>.parseFileContent(fileName: String): T {
    return jacksonObjectMapper().readValue(this.getFileTextContent(fileName))
  }
}
