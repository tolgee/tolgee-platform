package io.tolgee.formats.csv.out

import io.tolgee.dtos.IExportParams
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.csv.CsvEntry
import io.tolgee.formats.generic.IcuToGenericFormatMessageConvertor
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class CsvFileExporter(
  val translations: List<ExportTranslationView>,
  val exportParams: IExportParams,
  private val isProjectIcuPlaceholdersEnabled: Boolean = true,
) : FileExporter {
  private val fileName
    get() = exportParams.languages?.takeIf { it.size == 1 }?.iterator()?.next() ?: "exported"

  private val messageFormat
    get() = exportParams.messageFormat ?: ExportMessageFormat.ICU

  private val placeholderConvertorFactory
    get() = messageFormat.paramConvertorFactory

  val entries =
    translations.map {
      val converted = convertMessage(it.text, it.key.isPlural)
      CsvEntry(
        key = it.key.name,
        language = it.languageTag,
        value = converted,
      )
    }

  private fun convertMessage(
    text: String?,
    isPlural: Boolean,
  ): String? {
    return getMessageConvertor(text, isPlural).convert()
  }

  private fun getMessageConvertor(
    text: String?,
    isPlural: Boolean,
  ) = IcuToGenericFormatMessageConvertor(
    text,
    isPlural,
    isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
    paramConvertorFactory = placeholderConvertorFactory,
  )

  override fun produceFiles(): Map<String, InputStream> {
    return mapOf(
      "$fileName.csv" to entries.toCsv(),
    )
  }

  private fun List<CsvEntry>.toCsv(): InputStream {
    val languageTags =
      exportParams.languages?.sorted()?.toTypedArray()
        ?: this.map { it.language }.distinct().sorted().toTypedArray()
    return CsvFileWriter(
      languageTags = languageTags,
      data = this,
      delimiter = ',',
    ).produceFiles()
  }
}
