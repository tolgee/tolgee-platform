package io.tolgee.unit.util

import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.testing.assert

fun Map<String, String>.assertFile(
  file: String,
  content: String,
) {
  this[file]!!.assert.isEqualTo(content)
}

fun getExported(exporter: FileExporter): Map<String, String> {
  val files = exporter.produceFiles()
  val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
  return data
}
