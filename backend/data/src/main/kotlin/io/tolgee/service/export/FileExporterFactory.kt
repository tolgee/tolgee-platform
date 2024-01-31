package io.tolgee.service.export

import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.export.ExportFormat
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import io.tolgee.service.export.exporters.JsonFileExporter
import io.tolgee.service.export.exporters.PoFileExporter
import io.tolgee.service.export.exporters.XliffFileExporter
import org.springframework.stereotype.Component

@Component
class FileExporterFactory {
  fun create(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.JSON -> JsonFileExporter(data, exportParams)
      ExportFormat.XLIFF -> XliffFileExporter(data, exportParams, baseTranslationsProvider, baseLanguage)
      ExportFormat.PO_PHP ->
        PoFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          SupportedFormat.PHP,
        )
      ExportFormat.PO_C -> PoFileExporter(data, exportParams, baseTranslationsProvider, baseLanguage, SupportedFormat.C)
      ExportFormat.PO_PYTHON ->
        PoFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          SupportedFormat.PYTHON,
        )
    }
  }
}
