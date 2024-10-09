package io.tolgee.formats.csv.`in`

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import io.tolgee.formats.csv.CsvEntry
import java.io.InputStream

class CsvFileParser(
  private val inputStream: InputStream,
  private val delimiter: Char,
  private val languageFallback: String,
) {
  val rawData: List<Array<String>> by lazy {
    val inputReader = inputStream.reader()
    val parser = CSVParserBuilder().withSeparator(delimiter).build()
    val reader = CSVReaderBuilder(inputReader).withCSVParser(parser).build()

    return@lazy reader.readAll()
  }

  val headers: Array<String>? by lazy {
    rawData.firstOrNull()
  }

  val languages: List<String> by lazy {
    headers?.takeIf { it.size > 1 }?.drop(1) ?: emptyList()
  }

  val languagesWithFallback: Sequence<String>
    get() = languages.asSequence().plus(generateSequence { languageFallback })

  val rows: List<Array<String>> by lazy {
    rawData.takeIf { it.size > 1 }?.drop(1) ?: emptyList()
  }

  fun Array<String>.rowToCsvEntries(): Sequence<CsvEntry> {
    if (isEmpty()) {
      return emptySequence()
    }
    val keyName = getOrNull(0) ?: ""
    if (size == 1) {
      return sequenceOf(CsvEntry(keyName, languageFallback, null))
    }
    val translations = drop(1).asSequence()
    return translations
      .zip(languagesWithFallback)
      .map { (translation, languageTag) ->
        CsvEntry(
          keyName,
          languageTag,
          translation,
        )
      }
  }

  fun parse(): List<CsvEntry> {
    return rows.flatMap {
      it.rowToCsvEntries()
    }
  }
}
