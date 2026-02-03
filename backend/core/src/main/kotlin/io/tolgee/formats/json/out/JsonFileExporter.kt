package io.tolgee.formats.json.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.genericStructuredFile.out.GenericStructuredFileExporter
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class JsonFileExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  projectIcuPlaceholdersSupport: Boolean,
  val objectMapper: ObjectMapper,
  customPrettyPrinter: CustomPrettyPrinter,
  filePathProvider: ExportFilePathProvider,
) : FileExporter {
  private val messageFormat =
    when (exportParams.format) {
      ExportFormat.JSON_TOLGEE -> ExportMessageFormat.ICU
      ExportFormat.JSON_I18NEXT -> ExportMessageFormat.I18NEXT
      ExportFormat.APPLE_SDK -> ExportMessageFormat.APPLE_SPRINTF
      ExportFormat.ANDROID_SDK -> ExportMessageFormat.JAVA_STRING_FORMAT
      else -> exportParams.messageFormat ?: ExportMessageFormat.ICU
    }

  private val genericExporter =
    GenericStructuredFileExporter(
      translations = translations,
      exportParams = exportParams,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
      objectMapper = objectMapper,
      supportArrays = supportArrays,
      messageFormat = messageFormat,
      customPrettyPrinter = customPrettyPrinter,
      filePathProvider = filePathProvider,
    )

  private val supportArrays
    get() =
      when (exportParams.format) {
        ExportFormat.JSON_TOLGEE -> false
        ExportFormat.APPLE_SDK -> false
        ExportFormat.ANDROID_SDK -> true
        else -> exportParams.supportArrays
      }

  val result: LinkedHashMap<String, StructureModelBuilder> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    return genericExporter.produceFiles()
  }
}
