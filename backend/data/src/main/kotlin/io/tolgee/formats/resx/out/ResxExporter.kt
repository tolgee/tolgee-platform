package io.tolgee.formats.resx.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.IcuToIcuPlaceholderConvertor
import io.tolgee.formats.generic.IcuToGenericFormatMessageConvertor
import io.tolgee.formats.resx.ResxEntry
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class ResxExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
  private val pathProvider: ExportFilePathProvider,
) : FileExporter {
  private val fileUnits = mutableMapOf<String, MutableList<ResxEntry>>()

  private fun getModels(): Map<String, List<ResxEntry>> {
    prepare()
    return fileUnits
  }

  private fun prepare() {
    translations.forEach { translation ->
      val converted = getConvertedMessage(translation)
      val entry =
        ResxEntry(
          key = translation.key.name,
          data = converted,
          comment = translation.description,
        )

      val units = getFileUnits(translation)
      units.add(entry)
    }
  }

  private fun getFileUnits(translation: ExportTranslationView): MutableList<ResxEntry> {
    val filePath =
      pathProvider.getFilePath(
        languageTag = translation.languageTag,
        namespace = translation.key.namespace,
      )
    return fileUnits.computeIfAbsent(filePath) { mutableListOf() }
  }

  private fun getConvertedMessage(
    translation: ExportTranslationView,
    isPlural: Boolean = translation.key.isPlural,
  ): String? {
    return IcuToGenericFormatMessageConvertor(
      translation.text,
      isPlural,
      isProjectIcuPlaceholdersEnabled,
    ) {
      IcuToIcuPlaceholderConvertor()
    }.convert()
  }

  override fun produceFiles(): Map<String, InputStream> {
    return getModels()
      .map { (path, model) ->
        path to ResxWriter(model).produceFiles()
      }.toMap()
  }
}
