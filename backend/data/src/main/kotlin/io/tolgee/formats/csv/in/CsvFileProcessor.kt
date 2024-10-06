package io.tolgee.formats.csv.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.csv.CsvEntry
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class CsvFileProcessor(
  override val context: FileProcessorContext,
) : ImportFileProcessor() {
  override fun process() {
    val data = parse()
    val format = getFormat(data)
    data.importAll(format)
  }

  fun Iterable<CsvEntry>.importAll(format: ImportFormat) {
    forEachIndexed { idx, it -> it.import(idx, format) }
  }

  fun CsvEntry.import(
    index: Int,
    format: ImportFormat,
  ) {
    val selectedLanguage = language ?: firstLanguageTagGuessOrUnknown
    val converted =
      format.messageConvertor.convert(
        value,
        selectedLanguage,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )
    context.addTranslation(
      key,
      selectedLanguage,
      converted.message,
      index,
      pluralArgName = converted.pluralArgName,
      rawData = value,
      convertedBy = format,
    )
  }

  private fun parse() =
    try {
      // TODO: make delimiter configurable
      CsvFileParser(context.file.data.inputStream(), ';').parse()
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
    }

  private fun getFormat(data: Any?): ImportFormat {
    return context.mapping?.format ?: CSVImportFormatDetector().detectFormat(data)
  }
}
