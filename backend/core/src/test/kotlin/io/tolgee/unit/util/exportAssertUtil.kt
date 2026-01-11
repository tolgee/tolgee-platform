package io.tolgee.unit.util

import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.testing.assert
import java.util.zip.ZipInputStream

fun Map<String, String>.assertFile(
  file: String,
  content: String,
) {
  this[file]!!.assert.isEqualToNormalizingNewlines(content)
}

fun getExported(exporter: FileExporter): Map<String, String> {
  val files = exporter.produceFiles()
  val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
  return data
}

fun getExportedCompressed(exporter: FileExporter): Map<String, String> {
  val files = exporter.produceFiles()
  val data =
    files
      .map {
        it.key to
          buildString {
            val stream = ZipInputStream(it.value)
            var entry = stream.nextEntry
            while (entry != null) {
              appendLine("====================")
              appendLine(entry.name)
              appendLine("--------------------")
              append(stream.bufferedReader().readText())
              appendLine()
              entry = stream.nextEntry
            }
          }
      }.toMap()
  return data
}
