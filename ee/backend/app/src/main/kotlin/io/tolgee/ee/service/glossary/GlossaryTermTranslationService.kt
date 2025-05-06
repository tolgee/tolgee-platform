package io.tolgee.ee.service.glossary

import io.tolgee.constants.Message
import io.tolgee.ee.data.glossary.UpdateGlossaryTermTranslationRequest
import io.tolgee.ee.repository.glossary.GlossaryTermTranslationRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.stereotype.Service
import java.util.*

@Service
class GlossaryTermTranslationService(
  private val glossaryTermTranslationRepository: GlossaryTermTranslationRepository,
) {
  fun getDistinctLanguageTags(
    organizationId: Long,
    glossaryId: Long,
  ): Set<String> {
    return glossaryTermTranslationRepository.findDistinctLanguageTagsByGlossary(organizationId, glossaryId)
  }

  fun create(
    term: GlossaryTerm,
    dto: UpdateGlossaryTermTranslationRequest,
  ): GlossaryTermTranslation? {
    if (dto.text.isEmpty()) {
      return null
    }

    val translation =
      GlossaryTermTranslation(
        languageTag = dto.languageTag,
        text = dto.text,
      ).apply {
        this.term = term
      }
    return glossaryTermTranslationRepository.save(translation)
  }

  fun updateOrCreate(
    term: GlossaryTerm,
    dto: UpdateGlossaryTermTranslationRequest,
  ): GlossaryTermTranslation? {
    if (term.flagNonTranslatable && dto.languageTag != term.glossary.baseLanguageTag) {
      throw BadRequestException(Message.GLOSSARY_NON_TRANSLATABLE_TERM_CANNOT_BE_TRANSLATED)
    }

    if (dto.text.isEmpty()) {
      glossaryTermTranslationRepository.deleteByTermAndLanguageTag(term, dto.languageTag)
      return null
    }

    val translation = glossaryTermTranslationRepository.findByTermAndLanguageTag(term, dto.languageTag)
    if (translation == null) {
      return create(term, dto)
    }

    translation.text = dto.text
    return glossaryTermTranslationRepository.save(translation)
  }

  fun deleteAllNonBaseTranslations(term: GlossaryTerm) {
    glossaryTermTranslationRepository.deleteAllByTermAndLanguageTagIsNot(term, term.glossary.baseLanguageTag!!)
  }

  fun find(
    term: GlossaryTerm,
    languageTag: String,
  ): GlossaryTermTranslation? {
    return glossaryTermTranslationRepository.findByTermAndLanguageTag(term, languageTag)
  }

  fun findAll(
    organizationId: Long,
    projectId: Long,
    words: Set<String>,
    languageTag: String,
  ): Set<GlossaryTermTranslation> {
    val locale = Locale.forLanguageTag(languageTag) ?: Locale.ROOT
    return glossaryTermTranslationRepository
      .findByFirstWordLowercasedAndLanguageTagAndAssignedProjectIdAndOrganizationId(
        words.map { it.lowercase(locale) },
        languageTag,
        projectId,
        organizationId,
      )
  }

  fun get(
    term: GlossaryTerm,
    languageTag: String,
  ): GlossaryTermTranslation {
    return find(term, languageTag) ?: throw NotFoundException(Message.GLOSSARY_TERM_TRANSLATION_NOT_FOUND)
  }
}
