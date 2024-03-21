package io.tolgee.formats.yaml.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.NoOpFromIcuPlaceholderConvertor
import io.tolgee.formats.android.out.IcuToJavaPlaceholderConvertor
import io.tolgee.formats.genericStructuredFile.out.GenericStructuredFileExporter
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.formats.paramConvertors.out.IcuToRubyPlaceholderConvertor
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class YamlFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  objectMapper: ObjectMapper,
  projectIcuPlaceholdersSupport: Boolean,
) : FileExporter {
  override val fileExtension: String = exportParams.format.extension

  private val genericExporter =
    GenericStructuredFileExporter(
      translations = translations,
      exportParams = exportParams,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
      fileExtension = fileExtension,
      objectMapper = objectMapper,
      rootKeyIsLanguageTag = rootKeyIsLanguageTag,
      pluralsViaNesting = pluralsViaNesting,
      placeholderConvertorFactory = placeholderConvertorFactory,
    )

  private val pluralsViaNesting get() = exportParams.format in arrayOf(ExportFormat.YAML_RUBY, ExportFormat.YAML_JAVA)
  private val rootKeyIsLanguageTag get() = exportParams.format in arrayOf(ExportFormat.YAML_RUBY)
  private val placeholderConvertorFactory
    get() =
      when (exportParams.format) {
        ExportFormat.YAML_RUBY -> (
          {
            IcuToRubyPlaceholderConvertor()
          }
        )

        ExportFormat.YAML_JAVA -> (
          {
            IcuToJavaPlaceholderConvertor()
          }
        )

        else -> (
          {
            NoOpFromIcuPlaceholderConvertor()
          }
        )
      }

  val result: LinkedHashMap<String, StructureModelBuilder> =
    LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    return genericExporter.produceFiles()
  }
}
