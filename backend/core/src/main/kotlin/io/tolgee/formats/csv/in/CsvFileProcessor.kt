package io.tolgee.formats.csv.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.`in`.TableProcessor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.util.CsvDelimiterDetector

class CsvFileProcessor(
  override val context: FileProcessorContext,
) : TableProcessor(context) {
  override fun parse(): Pair<Iterable<TableEntry>, ImportFormat> {
    try {
      val detector = CsvDelimiterDetector(context.file.data.inputStream())
      val parser =
        CsvFileParser(
          inputStream = context.file.data.inputStream(),
          delimiter = detector.delimiter,
          languageFallback = firstLanguageTagGuessOrUnknown,
        )
      val data = parser.parse()
      val format = getFormat(parser.tableParser.rows)
      return data to format
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
    }
  }

  private fun getFormat(data: Any?): ImportFormat {
    return context.mapping?.format ?: CSVImportFormatDetector().detectFormat(data)
  }
}
