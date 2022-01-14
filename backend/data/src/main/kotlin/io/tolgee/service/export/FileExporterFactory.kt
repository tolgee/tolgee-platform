package io.tolgee.service.export

import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.model.Language
import io.tolgee.model.translation.Translation
import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.service.export.exporters.JsonFileExporter
import io.tolgee.service.export.exporters.XliffFileExporter
import org.springframework.stereotype.Component

@Component
class FileExporterFactory {
  fun create(
    data: List<Translation>,
    exportParams: ExportParams,
    baseTranslationsProvider: () -> List<Translation>,
    baseLanguage: Language
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.JSON -> JsonFileExporter(data, exportParams)
      ExportFormat.XLIFF -> XliffFileExporter(data, exportParams, baseTranslationsProvider, baseLanguage)
    }
  }
}
