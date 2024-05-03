package io.tolgee.formats.json.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.genericStructuredFile.out.GenericStructuredFileExporter
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class JsonFileExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  projectIcuPlaceholdersSupport: Boolean,
  val objectMapper: ObjectMapper,
) : FileExporter {
  private val fileExtension: String = ExportFormat.JSON.extension

  private val messageFormat =
    when (exportParams.format) {
      ExportFormat.JSON_TOLGEE -> ExportMessageFormat.ICU
      else -> exportParams.messageFormat ?: ExportMessageFormat.ICU
    }

  private val genericExporter =
    GenericStructuredFileExporter(
      translations = translations,
      exportParams = exportParams,
      fileExtension = fileExtension,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
      objectMapper = objectMapper,
      supportArrays = supportArrays,
      messageFormat = messageFormat,
    )

  private val supportArrays
    get() =
      when (exportParams.format) {
        ExportFormat.JSON_TOLGEE -> false
        else -> exportParams.supportArrays
      }

  val result: LinkedHashMap<String, StructureModelBuilder> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    return genericExporter.produceFiles()
  }
}
