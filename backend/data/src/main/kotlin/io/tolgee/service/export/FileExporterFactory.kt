package io.tolgee.service.export

import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.model.Language
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.service.export.exporters.JsonFileExporter
import io.tolgee.service.export.exporters.XliffFileExporter
import org.springframework.stereotype.Component

@Component
class FileExporterFactory {
  fun create(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: Language
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.JSON -> JsonFileExporter(data, exportParams)
      ExportFormat.XLIFF -> XliffFileExporter(data, exportParams, baseTranslationsProvider, baseLanguage)
    }
  }
}
