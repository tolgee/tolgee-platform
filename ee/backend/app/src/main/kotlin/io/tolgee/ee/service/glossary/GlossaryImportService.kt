package io.tolgee.ee.service.glossary

import io.tolgee.model.glossary.Glossary
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.io.InputStream
import io.tolgee.ee.service.glossary.formats.ImportGlossaryTerm
import io.tolgee.ee.service.glossary.formats.csv.`in`.GlossaryCSVParser
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import kotlin.reflect.KMutableProperty0

@Service
class GlossaryImportService(
  private val glossaryTermService: GlossaryTermService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
) {
  @Transactional
  fun importCsv(
    glossary: Glossary,
    inputStream: InputStream,
    delimiter: Char = ',',
  ): Int {
    val parsed = GlossaryCSVParser(inputStream, delimiter).parse()

    val terms = parsed.map {
      GlossaryTerm().apply {
        this.glossary = glossary
      }.applyFrom(it)
    }

    val translations = terms.flatMap { it.translations }

    glossaryTermService.saveAll(terms)
    glossaryTermTranslationService.saveAll(translations)

    return terms.size
  }

  private fun GlossaryTerm.applyFrom(glossaryTerm: ImportGlossaryTerm): GlossaryTerm {
    if (glossaryTerm.term != null) {
      translations.add(
        GlossaryTermTranslation(glossary.baseLanguageTag, glossaryTerm.term)
          .apply { term = this@applyFrom }
      )
    }

    ::description setIfNotNull glossaryTerm.description
    ::flagNonTranslatable setIfNotNull glossaryTerm.flagNonTranslatable
    ::flagCaseSensitive setIfNotNull glossaryTerm.flagCaseSensitive
    ::flagAbbreviation setIfNotNull glossaryTerm.flagAbbreviation
    ::flagForbiddenTerm setIfNotNull glossaryTerm.flagForbiddenTerm

    glossaryTerm.translations.forEach { (languageTag, text) ->
      translations.add(GlossaryTermTranslation(languageTag, text).apply { term = this@applyFrom })
    }

    return this
  }

  private infix fun <V> KMutableProperty0<V>.setIfNotNull(other: V?) = other?.let { set(it) }
}
