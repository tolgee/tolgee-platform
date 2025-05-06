package io.tolgee.ee.service.glossary

import io.tolgee.component.machineTranslation.metadata.TranslationGlossaryItem
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.data.glossary.*
import io.tolgee.ee.repository.glossary.GlossaryTermRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import io.tolgee.model.glossary.GlossaryTermTranslation.Companion.WORD_REGEX
import io.tolgee.service.machineTranslation.MtGlossaryTermsProvider
import io.tolgee.util.findAll
import jakarta.transaction.Transactional
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

@Primary
@Service
class GlossaryTermService(
  private val glossaryTermRepository: GlossaryTermRepository,
  private val glossaryService: GlossaryService,
  private val glossaryTermTranslationService: GlossaryTermTranslationService,
) : MtGlossaryTermsProvider {
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
    languageTags: Set<String>?,
  ): Page<GlossaryTerm> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    return glossaryTermRepository.findByGlossaryPaged(glossary, pageable, search, languageTags)
  }

  fun findAllIds(
    organizationId: Long,
    glossaryId: Long,
    search: String?,
    languageTags: Set<String>?,
  ): List<Long> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    return glossaryTermRepository.findAllIds(glossary, search, languageTags)
  }

//  fun findAllPagedWithTranslations(
//    organizationId: Long,
//    glossaryId: Long,
//    pageable: Pageable,
//    search: String?,
//    languageTags: Set<String>?,
//  ): Page<GlossaryTermWithTranslationsView> {
//    val glossary = glossaryService.get(organizationId, glossaryId)
//    return glossaryTermRepository.findByGlossaryPagedWithTranslations(glossary, pageable, search, languageTags)
//  }

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
  ): GlossaryTerm {
    val glossary = glossaryService.get(organizationId, glossaryId)
    val glossaryTerm =
      GlossaryTerm(
        description = request.description,
      ).apply {
        this.glossary = glossary
        description = request.description

        flagNonTranslatable = request.flagNonTranslatable
        flagCaseSensitive = request.flagCaseSensitive
        flagAbbreviation = request.flagAbbreviation
        flagForbiddenTerm = request.flagForbiddenTerm
      }
    return glossaryTermRepository.save(glossaryTerm)
  }

  fun createWithTranslation(
    organizationId: Long,
    glossaryId: Long,
    request: CreateGlossaryTermWithTranslationRequest,
  ): Pair<GlossaryTerm, GlossaryTermTranslation?> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    val glossaryTerm = create(organizationId, glossaryId, request)
    val translation =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = glossary.baseLanguageTag!!
        text = request.text
      }
    return glossaryTerm to
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
    if (!glossaryTerm.flagNonTranslatable && dto.flagNonTranslatable == true) {
      glossaryTermTranslationService.deleteAllNonBaseTranslations(glossaryTerm)
    }
    glossaryTerm.apply {
      description = dto.description
      flagNonTranslatable = dto.flagNonTranslatable ?: flagNonTranslatable
      flagCaseSensitive = dto.flagCaseSensitive ?: flagCaseSensitive
      flagAbbreviation = dto.flagAbbreviation ?: flagAbbreviation
      flagForbiddenTerm = dto.flagForbiddenTerm ?: flagForbiddenTerm
    }
    return glossaryTermRepository.save(glossaryTerm)
  }

  fun updateWithTranslation(
    organizationId: Long,
    glossaryId: Long,
    termId: Long,
    request: UpdateGlossaryTermWithTranslationRequest,
  ): Pair<GlossaryTerm, GlossaryTermTranslation?> {
    val glossary = glossaryService.get(organizationId, glossaryId)
    val glossaryTerm = update(organizationId, glossaryId, termId, request)
    val translationText = request.text
    if (translationText == null) {
      return glossaryTerm to
        glossaryTermTranslationService.find(
          glossaryTerm,
          glossary.baseLanguageTag!!,
        )
    }

    val translation =
      UpdateGlossaryTermTranslationRequest().apply {
        languageTag = glossary.baseLanguageTag!!
        text = translationText
      }
    return glossaryTerm to
      glossaryTermTranslationService.updateOrCreate(
        glossaryTerm,
        translation,
      )
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

  fun deleteMultiple(
    organizationId: Long,
    glossaryId: Long,
    termIds: Collection<Long>,
  ) {
    val glossary = glossaryService.get(organizationId, glossaryId)
    deleteMultiple(glossary, termIds)
  }

  fun deleteMultiple(
    glossary: Glossary,
    termIds: Collection<Long>,
  ) {
    glossaryTermRepository.deleteByGlossaryAndIdIn(glossary, termIds)
  }

  fun getHighlights(
    projectId: Long,
    text: String,
    languageTag: String,
  ): Set<GlossaryTermHighlight> {
    val words = text.findAll(WORD_REGEX).filter { it.isNotEmpty() }.toSet()
    val translations = glossaryTermTranslationService.findAll(projectId, words, languageTag)

    val locale = Locale.forLanguageTag(languageTag) ?: Locale.ROOT
    val textLowercased = text.lowercase(locale)

    return translations.flatMap { translation ->
      findTranslationPositions(text, textLowercased, translation, locale).map { position ->
        GlossaryTermHighlight(position, translation)
      }
    }.toSet()
  }

  private fun findTranslationPositions(
    textOriginal: String,
    textLowercased: String,
    translation: GlossaryTermTranslation,
    locale: Locale,
  ): Sequence<Position> {
    val term = translation.text ?: return emptySequence()
    if (translation.term.flagCaseSensitive) {
      return findPositions(textOriginal, term)
    }

    val termLowercase = term.lowercase(locale)
    return findPositions(textLowercased, termLowercase)
  }

  private fun findPositions(
    text: String,
    search: String,
  ): Sequence<Position> {
    val regex = Regex.escape(search).toRegex()
    val matches = regex.findAll(text)
    return matches.map { Position(it.range.first, it.range.last + 1) }
  }

  @Transactional
  override fun glossaryTermsFor(
    project: ProjectDto,
    sourceLanguageTag: String,
    targetLanguageTag: String,
    text: String,
  ): Set<TranslationGlossaryItem> =
    getHighlights(project.id, text, sourceLanguageTag)
      .filter { !it.value.text.isNullOrEmpty() }
      .map {
        val term = it.value.term
        val targetTranslation = term.translations.find { it.languageTag == targetLanguageTag }
        TranslationGlossaryItem(
          source = it.value.text ?: "",
          target = targetTranslation?.text,
          description = term.description,
          isNonTranslatable = term.flagNonTranslatable,
          isCaseSensitive = term.flagCaseSensitive,
          isAbbreviation = term.flagAbbreviation,
          isForbiddenTerm = term.flagForbiddenTerm,
        )
      }.toSet()
}
