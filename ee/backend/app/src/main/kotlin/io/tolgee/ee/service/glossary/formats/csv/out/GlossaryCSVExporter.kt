package io.tolgee.ee.service.glossary.formats.csv.out

import com.opencsv.CSVWriterBuilder
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import java.io.InputStream
import java.io.StringWriter

class GlossaryCSVExporter(
    val glossary: Glossary,
    val terms: List<GlossaryTerm>,
    val languageTags: Set<String>,
    val delimiter: Char,
) {
    val languageTagsWithoutBaseLanguage by lazy { (languageTags - glossary.baseLanguageTag).sorted() }

    val headers by lazy {
        arrayOf(
            "Term",
            "Description",
            "Flagged as non-translatable",
            "Flagged as case-sensitive",
            "Flagged as abbreviation",
            "Flagged as forbidden term",
        ) + languageTagsWithoutBaseLanguage
    }

    fun GlossaryTerm.asColumns(): Array<String> {
        return arrayOf(
            translations.find { it.languageTag == glossary.baseLanguageTag }?.text ?: "",
            description,
            flagNonTranslatable.toString(),
            flagCaseSensitive.toString(),
            flagAbbreviation.toString(),
            flagForbiddenTerm.toString(),
        ) + languageTagsWithoutBaseLanguage.map { languageTag ->
            translations.find { it.languageTag == languageTag }?.text ?: ""
        }
    }

    fun export(): InputStream {
        val output = StringWriter()
        CSVWriterBuilder(output).withSeparator(delimiter).build().use { writer ->
            writer.writeNext(headers)
            terms.forEach { writer.writeNext(it.asColumns()) }
        }
        return output.toString().byteInputStream(Charsets.UTF_8)
    }
}
