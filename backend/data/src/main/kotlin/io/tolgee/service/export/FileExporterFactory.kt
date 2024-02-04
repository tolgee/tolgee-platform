package io.tolgee.service.export

import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.ios.out.IOsStringsStringsdictExporter
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.formats.xliff.out.XliffFileExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
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
      ExportFormat.PO ->
        getPoExporter(data, exportParams, baseTranslationsProvider, baseLanguage)
      ExportFormat.IOS_STRINGS_STRINGSDICT ->
        IOsStringsStringsdictExporter(data, exportParams)
    }
  }

  private fun getPoExporter(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
  ): PoFileExporter {
    val supportedFormat =
      when (exportParams.messageFormat) {
        null -> SupportedFormat.C
        ExportMessageFormat.PO_PHP -> SupportedFormat.PHP
        ExportMessageFormat.PO_C -> SupportedFormat.C
        ExportMessageFormat.PO_PYTHON -> SupportedFormat.PYTHON
      }
    return PoFileExporter(
      data,
      exportParams,
      baseTranslationsProvider,
      baseLanguage,
      supportedFormat,
    )
  }
}
