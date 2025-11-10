package io.tolgee.ee.service.glossary

import io.tolgee.constants.Message
import io.tolgee.ee.service.glossary.formats.ImportGlossaryTerm
import io.tolgee.ee.service.glossary.formats.csv.`in`.GlossaryCSVParser
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import io.tolgee.util.CsvDelimiterDetector
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.io.InputStream
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
  ): Int {
    val data = inputStream.readAllBytes()
    val parsed =
      try {
        val detector = CsvDelimiterDetector(data.inputStream())
        GlossaryCSVParser(data.inputStream(), detector.delimiter).parse()
      } catch (e: Exception) {
        throw BadRequestException(Message.FILE_PROCESSING_FAILED, cause = e)
      }

    val terms =
      parsed.map {
        GlossaryTerm()
          .apply {
            this.glossary = glossary
          }.applyFrom(it)
      }

    val translations = terms.flatMap { it.translations }

    glossaryTermService.saveAll(terms)
    glossaryTermTranslationService.saveAll(translations)

    return terms.size
  }

  private fun GlossaryTerm.applyFrom(glossaryTerm: ImportGlossaryTerm): GlossaryTerm {
    val baseTranslation = glossaryTerm.term ?: glossaryTerm.translations[glossary.baseLanguageTag]

    baseTranslation?.let {
      // We've found a base translation.
      translations.add(
        GlossaryTermTranslation(glossary.baseLanguageTag, it)
          .apply { term = this@applyFrom },
      )
    }

    ::description setIfNotNull glossaryTerm.description
    ::flagNonTranslatable setIfNotNull glossaryTerm.flagNonTranslatable
    ::flagCaseSensitive setIfNotNull glossaryTerm.flagCaseSensitive
    ::flagAbbreviation setIfNotNull glossaryTerm.flagAbbreviation
    ::flagForbiddenTerm setIfNotNull glossaryTerm.flagForbiddenTerm

    if (flagNonTranslatable) {
      // The term is non-translatable, so we only care about the base translation.
      return this
    }

    glossaryTerm.translations.forEach { (languageTag, text) ->
      if (languageTag == glossary.baseLanguageTag) {
        // We've already dealt with the base translation.
        return@forEach
      }
      translations.add(GlossaryTermTranslation(languageTag, text).apply { term = this@applyFrom })
    }

    return this
  }

  private infix fun <V> KMutableProperty0<V>.setIfNotNull(other: V?) = other?.let { set(it) }
}
