package io.tolgee.ee.service.glossary

import io.tolgee.constants.Message
import io.tolgee.ee.data.glossary.UpdateGlossaryTermTranslationRequest
import io.tolgee.ee.repository.glossary.GlossaryTermTranslationRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.stereotype.Service

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
        languageCode = dto.languageCode,
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
    if (dto.text.isEmpty()) {
      glossaryTermTranslationRepository.deleteByTermAndLanguageCode(term, dto.languageCode)
      return null
    }

    val translation = glossaryTermTranslationRepository.findByTermAndLanguageCode(term, dto.languageCode)
    if (translation == null) {
      return create(term, dto)
    }

    translation.text = dto.text
    return glossaryTermTranslationRepository.save(translation)
  }

  fun find(
    term: GlossaryTerm,
    languageCode: String,
  ): GlossaryTermTranslation? {
    return glossaryTermTranslationRepository.findByTermAndLanguageCode(term, languageCode)
  }

  fun get(
    term: GlossaryTerm,
    languageCode: String,
  ): GlossaryTermTranslation {
    return find(term, languageCode) ?: throw NotFoundException(Message.GLOSSARY_TERM_TRANSLATION_NOT_FOUND)
  }
}
