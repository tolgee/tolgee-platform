package io.tolgee.ee.service.glossary.formats.csv.`in`

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import io.tolgee.ee.service.glossary.formats.ImportGlossaryTerm
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_DESCRIPTION
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_FLAG_ABBREVIATION
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_FLAG_CASE_SENSITIVE
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_FLAG_FORBIDDEN
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_FLAG_TRANSLATABLE
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_TERM
import java.io.InputStream

class GlossaryCSVParser(
  val input: InputStream,
  val delimiter: Char = ',',
) {
  val reader: CSVReader by lazy {
    CSVReaderBuilder(input.reader())
      .withCSVParser(
        CSVParserBuilder().withSeparator(delimiter).build(),
      ).build()
  }

  val rawRows: List<Array<String>> by lazy { reader.readAll() }

  val headers: List<String>? by lazy {
    rawRows.firstOrNull()?.map { it.trim() }
  }

  val rows: List<Array<String>> by lazy {
    rawRows.drop(1)
  }

  val specialHeaderIndices by lazy {
    listOfNotNull(idxTerm, idxDescription, idxTranslatable, idxCaseSensitive, idxAbbreviation, idxForbiddenTerm)
  }

  val idxTerm by lazy { findHeaderIndex(GLOSSARY_CSV_HEADER_TERM) }
  val idxDescription by lazy { findHeaderIndex(GLOSSARY_CSV_HEADER_DESCRIPTION) }
  val idxTranslatable by lazy { findHeaderIndex(GLOSSARY_CSV_HEADER_FLAG_TRANSLATABLE) }
  val idxCaseSensitive by lazy { findHeaderIndex(GLOSSARY_CSV_HEADER_FLAG_CASE_SENSITIVE) }
  val idxAbbreviation by lazy { findHeaderIndex(GLOSSARY_CSV_HEADER_FLAG_ABBREVIATION) }
  val idxForbiddenTerm by lazy { findHeaderIndex(GLOSSARY_CSV_HEADER_FLAG_FORBIDDEN) }

  val idxTranslations by lazy {
    (0 until (headers?.size ?: 0)).filter { it !in specialHeaderIndices }
  }

  fun parse(): List<ImportGlossaryTerm> {
    return rows.mapNotNull { it.asGlossaryTerm() }
  }

  fun Array<String>.asGlossaryTerm(): ImportGlossaryTerm? {
    return ImportGlossaryTerm(
      term = getSafe(idxTerm),
      description = getSafe(idxDescription),
      // stored inverted - as translatable
      flagNonTranslatable = parseBoolean(getSafe(idxTranslatable))?.let { !it },
      flagCaseSensitive = parseBoolean(getSafe(idxCaseSensitive)),
      flagAbbreviation = parseBoolean(getSafe(idxAbbreviation)),
      flagForbiddenTerm = parseBoolean(getSafe(idxForbiddenTerm)),
      translations =
        idxTranslations
          .map { headers!![it] to getSafe(it).orEmpty() }
          .filter { it.second.isNotBlank() }
          .toMap(),
    ).takeIf {
      // Ignore empty rows
      it.term != null || it.description != null || it.translations.isNotEmpty()
    }
  }

  private fun findHeaderIndex(name: String): Int? {
    return headers?.indexOfFirst { it.equals(name, ignoreCase = true) }?.takeIf { it >= 0 }
  }

  fun Array<String>.getSafe(idx: Int?): String? {
    if (idx == null) return null
    if (idx >= size) return null
    return this[idx].ifBlank { null }
  }

  private fun parseBoolean(value: String?): Boolean? {
    if (value.isNullOrBlank()) return null
    return when (value.trim().lowercase()) {
      "true", "1", "yes", "y", "t" -> true
      "false", "0", "no", "n", "f" -> false
      else -> false
    }
  }
}
