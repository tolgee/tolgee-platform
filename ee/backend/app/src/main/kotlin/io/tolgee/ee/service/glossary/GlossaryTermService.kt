package io.tolgee.ee.service.glossary

import io.tolgee.constants.Message
import io.tolgee.ee.data.glossary.CreateGlossaryTermRequest
import io.tolgee.ee.data.glossary.CreateGlossaryTermTranslationRequest
import io.tolgee.ee.data.glossary.UpdateGlossaryTermRequest
import io.tolgee.ee.repository.glossary.GlossaryTermRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GlossaryTermService(
  private val glossaryTermRepository: GlossaryTermRepository,
  private val glossaryService: GlossaryService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
) {
  fun find(
    organizationId: Long,
    glossaryId: Long,
    termId: Long,
  ): GlossaryTerm? {
    return glossaryTermRepository.find(organizationId, glossaryId, termId)
  }

  fun findAll(
    organizationId: Long,
    glossaryId: Long,
  ): List<GlossaryTerm> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    return glossaryTermRepository.findByGlossary(glossary)
  }

  fun findAllPaged(
    organizationId: Long,
    glossaryId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<GlossaryTerm> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    return glossaryTermRepository.findByGlossaryPaged(glossary, pageable, search)
  }

  fun get(
    organizationId: Long,
    glossaryId: Long,
    termId: Long,
  ): GlossaryTerm {
    return find(organizationId, glossaryId, termId)
      ?: throw NotFoundException(Message.GLOSSARY_TERM_NOT_FOUND)
  }

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

  fun update(
    organizationId: Long,
    glossaryId: Long,
    termId: Long,
    dto: UpdateGlossaryTermRequest,
  ): GlossaryTerm {
    val glossaryTerm = get(organizationId, glossaryId, termId)
    glossaryTerm.apply {
      description = dto.description
      flagNonTranslatable = dto.flagNonTranslatable ?: flagNonTranslatable
      flagCaseSensitive = dto.flagCaseSensitive ?: flagCaseSensitive
      flagAbbreviation = dto.flagAbbreviation ?: flagAbbreviation
      flagForbiddenTerm = dto.flagForbiddenTerm ?: flagForbiddenTerm
    }
    return glossaryTermRepository.save(glossaryTerm)
  }

  fun delete(
    organizationId: Long,
    glossaryId: Long,
    termId: Long,
  ) {
    val glossaryTerm = get(organizationId, glossaryId, termId)
    delete(glossaryTerm)
  }

  fun delete(glossaryTerm: GlossaryTerm) {
    glossaryTermRepository.delete(glossaryTerm)
  }
}
