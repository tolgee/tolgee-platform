package io.tolgee.ee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.ee.repository.TranslationSuggestionRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Project
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationSuggestionView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationService
import io.tolgee.service.translation.TranslationSuggestionService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
) : TranslationSuggestionService {
  override fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>
  ): Map<Long, List<TranslationSuggestionView>> {
    val data = translationSuggestionRepository.getByKeyId(projectId, languageIds, keyIds)
    val result = mutableMapOf<Long, MutableList<TranslationSuggestionView>>()
    data.forEach {
      val keyId = it.keyId
      val existing = result[keyId] ?: mutableListOf()
      existing.add(it)
      result.set(keyId, existing)
    }
    return result
  }

  fun createSuggestion(
    project: Project,
    languageTag: String,
    keyId: Long,
    dto: CreateTranslationSuggestionRequest,
  ): TranslationSuggestion {
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, project.id)
    val language =
      languageService.findEntity(project.id, languageTag) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

    val suggestion = TranslationSuggestion(
      key = key,
      project = project,
      language = language,
      author = authenticationFacade.authenticatedUserEntity,
      translation = dto.translation,
    )

    translationSuggestionRepository.save(suggestion)
    return suggestion
  }

  fun getSuggestion(projectId: Long, keyId: Long, suggestionId: Long): TranslationSuggestion {
    val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
    keyService.checkInProject(key, projectId)
    return translationSuggestionRepository.findById(suggestionId).getOrNull()
      ?: throw NotFoundException(Message.SUGGESTION_NOT_FOUND)
  }

  fun declineSuggestion(projectId: Long, keyId: Long, suggestionId: Long): TranslationSuggestion {
    val suggestion = getSuggestion(projectId, keyId, suggestionId)
    suggestion.state = TranslationSuggestionState.DECLINED
    translationSuggestionRepository.save(suggestion)
    return suggestion
  }

  fun reverseSuggestion(projectId: Long, keyId: Long, suggestionId: Long): TranslationSuggestion {
    val suggestion = getSuggestion(projectId, keyId, suggestionId)
    suggestion.state = TranslationSuggestionState.ACTIVE
    translationSuggestionRepository.save(suggestion)
    return suggestion
  }

  @Transactional
  fun acceptSuggestion(
    projectId: Long,
    languageTag: String,
    keyId: Long,
    suggestionId: Long,
    declineOther: Boolean
  ): Pair<TranslationSuggestion, List<Long>> {
    val language =
      languageService.findEntity(projectId, languageTag) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    val suggestion = getSuggestion(projectId, keyId, suggestionId)
    suggestion.state = TranslationSuggestionState.ACCEPTED
    val declined = if (declineOther) {
      declineOtherSuggestions(projectId, language.id, keyId, suggestionId)
    } else {
      emptyList()
    }
    translationSuggestionRepository.save(suggestion)
    translationService.setForKey(
      entityManager.getReference(Key::class.java, keyId),
      mapOf(languageTag to suggestion.translation)
    )
    return Pair(suggestion, declined)
  }

  fun declineOtherSuggestions(
    projectId: Long, languageId: Long, keyId: Long, suggestionId: Long
  ): List<Long> {
    val toDecline = translationSuggestionRepository.getAllActive(projectId, languageId, keyId)
      .filter { it.id != suggestionId }
    toDecline.forEach { it.state = TranslationSuggestionState.DECLINED }
    translationSuggestionRepository.saveAll(toDecline)
    return toDecline.map { it.id }
  }

  fun getSuggestionsPaged(
    pageable: Pageable, projectId: Long, languageTag: String, keyId: Long, filters: SuggestionFilters,
  ): Page<TranslationSuggestion> {
    return translationSuggestionRepository.getPaged(
      pageable,
      projectId,
      languageTag,
      keyId,
      filters
    )
  }

  fun deleteSuggestionCreatedByUser(projectId: Long, keyId: Long, suggestionId: Long, userId: Long) {
    val suggestion = getSuggestion(projectId, keyId, suggestionId)

    if (suggestion.author?.id != userId) {
      throw PermissionException(Message.USER_CAN_ONLY_DELETE_HIS_SUGGESTIONS)
    }

    translationSuggestionRepository.delete(suggestion)
  }
}
