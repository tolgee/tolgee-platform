package io.tolgee.dtos.request.export

import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.service.export.exporters.JsonFileExporter
import io.tolgee.service.export.exporters.XliffFileExporter

enum class ExportFormat(val exporter: Class<out FileExporter>) {
  JSON(JsonFileExporter::class.java),
  XLIFF(XliffFileExporter::class.java),
}
