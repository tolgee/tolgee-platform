package io.tolgee.ee.service.glossary

import io.tolgee.ee.data.glossary.CreateGlossaryTermRequest
import io.tolgee.ee.data.glossary.CreateGlossaryTermTranslationRequest
import io.tolgee.ee.repository.glossary.GlossaryTermRepository
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.stereotype.Service

@Service
class GlossaryTermService(
  private val glossaryTermRepository: GlossaryTermRepository,
  private val glossaryService: GlossaryService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
) {
  fun create(
    organizationId: Long,
    glossaryId: Long,
    request: CreateGlossaryTermRequest,
  ): Pair<GlossaryTerm, GlossaryTermTranslation> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    val glossaryTerm =
      GlossaryTerm(
        description = request.description,
      ).apply {
        this.glossary = glossary
        this.description = request.description

        this.flagNonTranslatable = flagNonTranslatable
        this.flagCaseSensitive = flagCaseSensitive
        this.flagAbbreviation = flagAbbreviation
        this.flagForbiddenTerm = flagForbiddenTerm
      }
    val translation =
      CreateGlossaryTermTranslationRequest().apply {
        languageCode = glossary.baseLanguageCode!!
        text = request.text
      }
    return glossaryTermRepository.save(glossaryTerm) to
      glossaryTermTranslationService.create(
        glossaryTerm,
        translation,
      )
  }
}
