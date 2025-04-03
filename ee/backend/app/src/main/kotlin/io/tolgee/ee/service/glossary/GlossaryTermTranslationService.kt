package io.tolgee.ee.service.glossary

import io.tolgee.ee.data.glossary.CreateGlossaryTermTranslationRequest
import io.tolgee.ee.repository.glossary.GlossaryTermTranslationRepository
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
    dto: CreateGlossaryTermTranslationRequest,
  ): GlossaryTermTranslation {
    val translation =
      GlossaryTermTranslation(
        languageCode = dto.languageCode,
        text = dto.text,
      ).apply {
        this.term = term
      }
    return glossaryTermTranslationRepository.save(translation)
  }
}
