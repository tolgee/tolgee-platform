package io.tolgee.ee.service.glossary.formats.csv.out

import com.opencsv.CSVWriterBuilder
import io.tolgee.ee.service.glossary.formats.csv.GLOSSARY_CSV_HEADER_NAMES
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
        (GLOSSARY_CSV_HEADER_NAMES + languageTagsWithoutBaseLanguage).toTypedArray()
    }

    fun GlossaryTerm.asColumns(): Array<String> {
        return arrayOf(
            translations.find { it.languageTag == glossary.baseLanguageTag }?.text ?: "",
            description,
            (!flagNonTranslatable).asYesOrNo(), // stored inverted - as translatable
            flagCaseSensitive.asYesOrNo(),
            flagAbbreviation.asYesOrNo(),
            flagForbiddenTerm.asYesOrNo(),
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

    fun Boolean.asYesOrNo(): String {
        return if (this) "Yes" else "No"
    }
}
