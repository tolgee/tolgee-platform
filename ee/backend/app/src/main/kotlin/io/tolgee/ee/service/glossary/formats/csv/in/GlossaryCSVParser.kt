package io.tolgee.ee.service.glossary.formats.csv.`in`

import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import io.tolgee.ee.service.glossary.formats.ImportGlossaryTerm
import java.io.InputStream

class GlossaryCSVParser(
    val input: InputStream,
    val delimiter: Char = ',',
) {
    val reader: CSVReader by lazy {
        CSVReaderBuilder(input.reader()).withCSVParser(
            CSVParserBuilder().withSeparator(delimiter).build()
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
        listOfNotNull(idxTerm, idxDescription, idxNonTranslatable, idxCaseSensitive, idxAbbreviation, idxForbiddenTerm)
    }

    val idxTerm by lazy { findHeaderIndex("Term") }
    val idxDescription by lazy { findHeaderIndex("Description") }
    val idxNonTranslatable by lazy { findHeaderIndex("Flagged as non-translatable") }
    val idxCaseSensitive by lazy { findHeaderIndex("Flagged as case-sensitive") }
    val idxAbbreviation by lazy { findHeaderIndex("Flagged as abbreviation") }
    val idxForbiddenTerm by lazy { findHeaderIndex("Flagged as forbidden term") }

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
            flagNonTranslatable = parseBoolean(getSafe(idxNonTranslatable)),
            flagCaseSensitive = parseBoolean(getSafe(idxCaseSensitive)),
            flagAbbreviation = parseBoolean(getSafe(idxAbbreviation)),
            flagForbiddenTerm = parseBoolean(getSafe(idxForbiddenTerm)),
            translations = idxTranslations
                .map { headers!![it] to getSafe(it).orEmpty() }
                .filter { it.second.isNotEmpty() }
                .toMap(),
        ).takeIf {
            // Ignore empty rows
            !it.term.isNullOrBlank() || !it.description.isNullOrBlank() || it.translations.isNotEmpty()
        }
    }

    private fun findHeaderIndex(name: String): Int? {
        return headers?.indexOfFirst { it.equals(name, ignoreCase = true) }?.takeIf { it >= 0 }
    }

    fun Array<String>.getSafe(idx: Int?): String? {
        if (idx == null) return null
        if (idx >= size) return null
        return this[idx]
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
