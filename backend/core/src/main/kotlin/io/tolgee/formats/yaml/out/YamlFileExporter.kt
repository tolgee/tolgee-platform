package io.tolgee.formats.yaml.out

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

class YamlFileExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  objectMapper: ObjectMapper,
  projectIcuPlaceholdersSupport: Boolean,
  customPrettyPrinter: CustomPrettyPrinter,
  filePathProvider: ExportFilePathProvider,
) : FileExporter {
  private val messageFormat =
    when (exportParams.format) {
      ExportFormat.YAML_RUBY -> ExportMessageFormat.RUBY_SPRINTF
      else -> exportParams.messageFormat ?: ExportMessageFormat.ICU
    }

  private val genericExporter =
    GenericStructuredFileExporter(
      translations = translations,
      exportParams = exportParams,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
      objectMapper = objectMapper,
      rootKeyIsLanguageTag = rootKeyIsLanguageTag,
      supportArrays = supportArrays,
      messageFormat = messageFormat,
      customPrettyPrinter = customPrettyPrinter,
      filePathProvider = filePathProvider,
    )

  private val rootKeyIsLanguageTag
    get() = exportParams.format == ExportFormat.YAML_RUBY

  private val supportArrays
    get() =
      when (exportParams.format) {
        ExportFormat.YAML_RUBY -> true
        else -> exportParams.supportArrays
      }

  val result: LinkedHashMap<String, StructureModelBuilder> =
    LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    return genericExporter.produceFiles()
  }
}
