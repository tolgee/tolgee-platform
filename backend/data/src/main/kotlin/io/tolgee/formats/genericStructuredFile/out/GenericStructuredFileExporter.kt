package io.tolgee.formats.genericStructuredFile.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.FromIcuPlaceholderConvertor
import io.tolgee.formats.NoOpFromIcuPlaceholderConvertor
import io.tolgee.formats.generic.IcuToGenericFormatMessageConvertor
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class GenericStructuredFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  override val fileExtension: String,
  private val projectIcuPlaceholdersSupport: Boolean,
  private val objectMapper: ObjectMapper,
  private val placeholderConvertorFactory: (() -> FromIcuPlaceholderConvertor)?,
  private val rootKeyIsLanguageTag: Boolean = false,
  private val pluralsViaNesting: Boolean = false,
) : FileExporter {
  val result: LinkedHashMap<String, StructureModelBuilder> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result.asSequence().map { (fileName, modelBuilder) ->
      fileName to
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(modelBuilder.result)
          .inputStream()
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val fileContentResult = getFileContentResultBuilder(translation)
      fileContentResult.addValue(
        translation.languageTag,
        translation.key.name,
        convertMessage(translation.text, translation.key.isPlural),
      )
    }
  }

  private fun convertMessage(
    text: String?,
    isPlural: Boolean,
  ): String? {
    return IcuToGenericFormatMessageConvertor(
      text,
      isPlural,
      projectIcuPlaceholdersSupport,
      placeholderConvertorFactory ?: { NoOpFromIcuPlaceholderConvertor() },
    ).convert()
  }

  private fun getFileContentResultBuilder(translation: ExportTranslationView): StructureModelBuilder {
    val absolutePath = translation.getFilePath()
    return result.computeIfAbsent(absolutePath) {
      StructureModelBuilder(exportParams.structureDelimiter, exportParams.supportArrays)
    }
  }
}
