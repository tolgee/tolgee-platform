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
    val (data, format) = parse()
    data.importAll(format)
  }

  fun Iterable<CsvEntry>.importAll(format: ImportFormat) {
    forEachIndexed { idx, it -> it.import(idx, format) }
  }

  fun CsvEntry.import(
    index: Int,
    format: ImportFormat,
  ) {
    val converted =
      format.messageConvertor.convert(
        value,
        language,
        convertPlaceholders = context.importSettings.convertPlaceholdersToIcu,
        isProjectIcuEnabled = context.projectIcuPlaceholdersEnabled,
      )
    context.addTranslation(
      key,
      language,
      converted.message,
      index,
      pluralArgName = converted.pluralArgName,
      rawData = value,
      convertedBy = format,
    )
  }

  private fun parse(): Pair<Iterable<CsvEntry>, ImportFormat> {
    try {
      val detector = CsvDelimiterDetector(context.file.data.inputStream())
      val parser =
        CsvFileParser(
          inputStream = context.file.data.inputStream(),
          delimiter = detector.delimiter,
          languageFallback = firstLanguageTagGuessOrUnknown,
        )
      val data = parser.parse()
      val format = getFormat(parser.rows)
      return data to format
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
    }
  }

  private fun getFormat(data: Any?): ImportFormat {
    return context.mapping?.format ?: CSVImportFormatDetector().detectFormat(data)
  }
}
