package io.tolgee.unit.formats.yaml.out

import io.tolgee.unit.formats.yaml.out.YamlExportTestData.getAllFeaturesExporter
import io.tolgee.unit.formats.yaml.out.YamlExportTestData.getIcuPlaceholdersDisabledExporter
import io.tolgee.unit.formats.yaml.out.YamlExportTestData.getIcuPlaceholdersEnabledExporter
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import org.junit.jupiter.api.Test

class RubyYamlFileExporterTest {
  // generate this with:
  // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
  @Test
  fun `exports with all features`() {
    val exporter = getAllFeaturesExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |cs:
    |  this:
    |    is:
    |      nested:
    |        plural:
    |          one: "%{count} den %{icuParam}"
    |          few: "%{count} dny"
    |          other: "%{count} dní"
    |    '[1]':
    |      is:
    |        collision: "Colission"
    |  this-is-array:
    |  - object: "I will be first %<icuParam>d"
    |  indexed params: "I will be first %s, %s"
    |  named params: "I will be first %{param1}, %{param2}"
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |cs:
    |  key3:
    |    one: "# den {icuParam}"
    |    few: "# dny"
    |    other: "# dní"
    |  item: "I will be first {icuParam, number}"
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.yaml",
      """
    |---
    |cs:
    |  key3:
    |    one: "%{count} den %<icuParam>d"
    |    few: "%{count} dny"
    |    other: "%{count} dní"
    |  item: "I will be first {icuParam} %<hello>d"
    |
      """.trimMargin(),
    )
  }
}
