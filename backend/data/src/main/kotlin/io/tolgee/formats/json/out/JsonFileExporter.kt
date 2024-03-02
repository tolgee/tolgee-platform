package io.tolgee.formats.json.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.nestedStructureModel.StructureModelBuilder
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class JsonFileExporter(
  override val translations: List<ExportTranslationView>,
  override val exportParams: IExportParams,
  val convertMessage: (message: String?, isPlural: Boolean) -> String? = { message, _ -> message },
) : FileExporter {
  override val fileExtension: String = ExportFormat.JSON.extension

  val result: LinkedHashMap<String, StructureModelBuilder> = LinkedHashMap()

  override fun produceFiles(): Map<String, InputStream> {
    prepare()
    return result.asSequence().map { (fileName, modelBuilder) ->
      fileName to
        jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(modelBuilder.result)
          .inputStream()
    }.toMap()
  }

  private fun prepare() {
    translations.forEach { translation ->
      val fileContentResult = getFileContentResultBuilder(translation)
      fileContentResult.addValue(translation.key.name, convertMessage(translation.text, translation.key.isPlural))
    }
  }

  private fun getFileContentResultBuilder(translation: ExportTranslationView): StructureModelBuilder {
    val absolutePath = translation.getFilePath(translation.key.namespace)
    return result.computeIfAbsent(absolutePath) {
      StructureModelBuilder(exportParams.structureDelimiter, exportParams.supportArrays)
    }
  }
}
