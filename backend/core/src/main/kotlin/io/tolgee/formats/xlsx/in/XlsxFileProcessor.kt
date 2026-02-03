package io.tolgee.formats.xlsx.`in`

import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.`in`.TableProcessor
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.service.dataImport.processors.FileProcessorContext

class XlsxFileProcessor(
  override val context: FileProcessorContext,
) : TableProcessor(context) {
  override fun parse(): Pair<Iterable<TableEntry>, ImportFormat> {
    try {
      val parser =
        XlsxFileParser(
          inputStream = context.file.data.inputStream(),
          languageFallback = firstLanguageTagGuessOrUnknown,
        )
      val data = parser.parse()
      val format = getFormat(parser.rawData)
      return data to format
    } catch (e: Exception) {
      throw ImportCannotParseFileException(context.file.name, e.message ?: "", e)
    }
  }

  private fun getFormat(data: Any?): ImportFormat {
    return context.mapping?.format ?: XlsxImportFormatDetector().detectFormat(data)
  }
}
