package io.tolgee.ee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.ee.repository.TranslationSuggestionRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.formats.StringIsNotPluralException
import io.tolgee.formats.normalizePlurals
import io.tolgee.model.Project
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationSuggestionView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.SetTranslationTextUtil
import io.tolgee.service.translation.TranslationService
import io.tolgee.service.translation.TranslationSuggestionService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Primary
@Component
class TranslationSuggestionServiceEeImpl(
  private val translationSuggestionRepository: TranslationSuggestionRepository,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val entityManager: EntityManager,
  private val authenticationFacade: AuthenticationFacade,
  private val translationService: TranslationService,
  private val tolgeeProperties: TolgeeProperties,
) : TranslationSuggestionService {
  override fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Map<Pair<Long, String>, List<TranslationSuggestionView>> {
    val data = translationSuggestionRepository.getByKeyId(projectId, languageIds, keyIds)
    val result = mutableMapOf<Pair<Long, String>, MutableList<TranslationSuggestionView>>()
    data.forEach {
      val pair = Pair(it.keyId, it.languageTag)
      val existing = result[pair] ?: mutableListOf()
      existing.add(it)
      result.set(pair, existing)
    }
    return result as Map<Pair<Long, String>, List<TranslationSuggestionView>>
  }

  fun createSuggestion(
    project: Project,
    languageId: Long,
    keyId: Long,
    dto: CreateTranslationSuggestionRequest,
  ): TranslationSuggestion {
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, project.id)
    val language =
      languageService.findEntity(languageId, project.id) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

    val normalizedTranslation =
      try {
        if (key.isPlural) {
          normalizePlurals(mapOf("key" to dto.translation)).get("key") ?: ""
        } else {
          dto.translation
        }
      } catch (e: StringIsNotPluralException) {
        throw BadRequestException(Message.INVALID_PLURAL_FORM, e)
      }

    checkSuggestionValid(normalizedTranslation)

    val existingSuggestion =
      translationSuggestionRepository.findSuggestion(
        project.id,
        language.id,
        key.id,
        normalizedTranslation,
        key.isPlural,
      )

    if (existingSuggestion.isNotEmpty()) {
      throw BadRequestException(Message.DUPLICATE_SUGGESTION)
    }

    val suggestion =
      TranslationSuggestion(
        project = project,
        language = language,
        author = authenticationFacade.authenticatedUserEntity,
        translation = normalizedTranslation,
        isPlural = key.isPlural,
      )
    suggestion.key = key

    translationSuggestionRepository.save(suggestion)
    return suggestion
  }

  fun getSuggestion(
    projectId: Long,
    keyId: Long,
    suggestionId: Long,
  ): TranslationSuggestion {
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, projectId)
    return translationSuggestionRepository.findById(suggestionId).getOrNull()
      ?: throw NotFoundException(Message.SUGGESTION_NOT_FOUND)
  }

  fun declineSuggestion(
    projectId: Long,
    keyId: Long,
    suggestionId: Long,
  ): TranslationSuggestion {
    val suggestion = getSuggestion(projectId, keyId, suggestionId)
    suggestion.state = TranslationSuggestionState.DECLINED
    translationSuggestionRepository.save(suggestion)
    return suggestion
  }

  fun suggestionSetActive(
    projectId: Long,
    keyId: Long,
    suggestionId: Long,
  ): TranslationSuggestion {
    val suggestion = getSuggestion(projectId, keyId, suggestionId)
    suggestion.state = TranslationSuggestionState.ACTIVE
    translationSuggestionRepository.save(suggestion)
    return suggestion
  }

  @Transactional
  fun acceptSuggestion(
    projectId: Long,
    languageId: Long,
    keyId: Long,
    suggestionId: Long,
    declineOther: Boolean,
  ): Pair<TranslationSuggestion, List<Long>> {
    val language =
      languageService.findEntity(languageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    val suggestion = getSuggestion(projectId, keyId, suggestionId)
    if (key.isPlural != suggestion.isPlural) {
      if (suggestion.isPlural) {
        throw BadRequestException(Message.SUGGESTION_CANT_BE_PLURAL)
      } else {
        throw BadRequestException(Message.SUGGESTION_MUST_BE_PLURAL)
      }
    }
    suggestion.state = TranslationSuggestionState.ACCEPTED
    val declined =
      if (declineOther) {
        declineOtherSuggestions(projectId, language.id, keyId, suggestionId)
      } else {
        emptyList()
      }
    translationSuggestionRepository.save(suggestion)
    translationService.setForKey(
      entityManager.getReference(Key::class.java, keyId),
      mapOf(language.tag to suggestion.translation),
      options = SetTranslationTextUtil.Companion.Options(keepState = true),
    )
    return Pair(suggestion, declined)
  }

  fun declineOtherSuggestions(
    projectId: Long,
    languageId: Long,
    keyId: Long,
    suggestionId: Long,
  ): List<Long> {
    val toDecline =
      translationSuggestionRepository
        .getAllActive(projectId, languageId, keyId)
        .filter { it.id != suggestionId }
    toDecline.forEach { it.state = TranslationSuggestionState.DECLINED }
    translationSuggestionRepository.saveAll(toDecline)
    return toDecline.map { it.id }
  }

  fun getSuggestionsPaged(
    pageable: Pageable,
    projectId: Long,
    languageId: Long,
    keyId: Long,
    filters: SuggestionFilters,
  ): Page<TranslationSuggestion> {
    return translationSuggestionRepository.getPaged(
      pageable,
      projectId,
      languageId,
      keyId,
      filters,
    )
  }

  fun deleteSuggestionCreatedByUser(
    projectId: Long,
    keyId: Long,
    suggestionId: Long,
    userId: Long,
  ) {
    val suggestion = getSuggestion(projectId, keyId, suggestionId)

    if (suggestion.author?.id != userId) {
      throw PermissionException(Message.USER_CAN_ONLY_DELETE_HIS_SUGGESTIONS)
    }

    translationSuggestionRepository.delete(suggestion)
  }

  override fun deleteAllByLanguage(id: Long) {
    val suggestions = translationSuggestionRepository.getAllByLanguage(id)
    translationSuggestionRepository.deleteAll(suggestions)
  }

  override fun deleteAllByProject(id: Long) {
    val suggestions = translationSuggestionRepository.getAllByProject(id)
    translationSuggestionRepository.deleteAll(suggestions)
  }

  private fun checkSuggestionValid(text: String) {
    if (text.length > tolgeeProperties.maxTranslationTextLength) {
      throw BadRequestException(Message.TRANSLATION_TEXT_TOO_LONG, listOf(tolgeeProperties.maxTranslationTextLength))
    }
  }
}
