package io.tolgee.service.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Message
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.android.out.AndroidStringsXmlExporter
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
import io.tolgee.formats.apple.out.AppleXliffExporter
import io.tolgee.formats.flutter.out.FlutterArbFileExporter
import io.tolgee.formats.generic.IcuToGenericFormatMessageConvertor
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.formats.po.PoSupportedMessageFormat
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.formats.xliff.out.XliffFileExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import org.springframework.stereotype.Component

@Component
class FileExporterFactory(
  private val objectMapper: ObjectMapper,
) {
  fun create(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
    projectIcuPlaceholdersSupport: Boolean,
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.JSON ->
        JsonFileExporter(
          data,
          exportParams,
        ) { text, isPlural ->
          IcuToGenericFormatMessageConvertor(text, isPlural, projectIcuPlaceholdersSupport).convert()
        }

      ExportFormat.XLIFF -> XliffFileExporter(data, exportParams, baseTranslationsProvider, baseLanguage)
      ExportFormat.APPLE_XLIFF ->
        AppleXliffExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage.tag,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.ANDROID_XML -> AndroidStringsXmlExporter(data, exportParams, projectIcuPlaceholdersSupport)
      ExportFormat.PO ->
        getPoExporter(data, exportParams, baseTranslationsProvider, baseLanguage, projectIcuPlaceholdersSupport)

      ExportFormat.APPLE_STRINGS_STRINGSDICT ->
        AppleStringsStringsdictExporter(data, exportParams, projectIcuPlaceholdersSupport)

      ExportFormat.FLUTTER_ARB ->
        FlutterArbFileExporter(
          data,
          exportParams,
          baseLanguage.tag,
          objectMapper,
          projectIcuPlaceholdersSupport,
        )
    }
  }

  private fun getPoExporter(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
    projectIcuPlaceholdersSupport: Boolean,
  ): PoFileExporter {
    val poSupportedMessageFormat =
      when (exportParams.messageFormat) {
        null -> PoSupportedMessageFormat.C
        ExportMessageFormat.PHP_SPRINTF -> PoSupportedMessageFormat.PHP
        ExportMessageFormat.C_SPRINTF -> PoSupportedMessageFormat.C
        ExportMessageFormat.PYTHON_SPRINTF -> PoSupportedMessageFormat.PYTHON
        else -> throw BadRequestException(Message.UNSUPPORTED_PO_MESSAGE_FORMAT)
      }
    return PoFileExporter(
      data,
      exportParams,
      baseTranslationsProvider,
      baseLanguage,
      poSupportedMessageFormat,
      projectIcuPlaceholdersSupport,
    )
  }
}
