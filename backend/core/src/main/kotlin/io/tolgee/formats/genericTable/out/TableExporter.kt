package io.tolgee.formats.genericTable.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.generic.IcuToGenericFormatMessageConvertor
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

abstract class TableExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  val isProjectIcuPlaceholdersEnabled: Boolean = true,
  val pathProvider: ExportFilePathProvider,
) : FileExporter {
  val messageFormat
    get() = exportParams.messageFormat ?: ExportMessageFormat.ICU

  val placeholderConvertorFactory
    get() = messageFormat.paramConvertorFactory

  val entries =
    translations
      .map {
        val converted = convertMessage(it.text, it.key.isPlural)
        val path =
          pathProvider.getFilePath(it.key.namespace)
        val entry =
          TableEntry(
            key = it.key.name,
            language = it.languageTag,
            value = converted,
          )
        path to entry
      }.groupBy({ it.first }, { it.second })

  fun convertMessage(
    text: String?,
    isPlural: Boolean,
  ): String? {
    return getMessageConvertor(text, isPlural).convert()
  }

  fun getMessageConvertor(
    text: String?,
    isPlural: Boolean,
  ) = IcuToGenericFormatMessageConvertor(
    text,
    isPlural,
    isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
    paramConvertorFactory = placeholderConvertorFactory,
  )

  override fun produceFiles(): Map<String, InputStream> {
    return entries.mapValues { (_, entry) -> entry.toFileContents() }
  }

  abstract fun List<TableEntry>.toFileContents(): InputStream
}
