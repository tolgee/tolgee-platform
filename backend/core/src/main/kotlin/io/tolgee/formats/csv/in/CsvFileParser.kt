package io.tolgee.formats.csv.`in`

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import io.tolgee.formats.genericTable.TableEntry
import io.tolgee.formats.genericTable.`in`.TableParser
import java.io.InputStream

class CsvFileParser(
  private val inputStream: InputStream,
  private val delimiter: Char,
  private val languageFallback: String,
) {
  val rawData: List<List<String>> by lazy {
    val inputReader = inputStream.reader()
    val parser = CSVParserBuilder().withSeparator(delimiter).build()
    val reader = CSVReaderBuilder(inputReader).withCSVParser(parser).build()

    return@lazy reader.readAll().map { it.toList() }
  }

  val tableParser: TableParser by lazy {
    TableParser(rawData, languageFallback)
  }

  fun parse(): List<TableEntry> {
    return tableParser.parse()
  }
}
