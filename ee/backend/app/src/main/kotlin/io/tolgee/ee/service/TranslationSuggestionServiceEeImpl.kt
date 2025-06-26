package io.tolgee.ee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.ee.repository.TranslationSuggestionRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.views.TranslationSuggestionView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.translation.TranslationSuggestionService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Primary
@Component
class TranslationSuggestionServiceEeImpl(
    private val translationSuggestionRepository: TranslationSuggestionRepository,
    private val keyService: KeyService,
    private val languageService: LanguageService,
    private val entityManager: EntityManager,
    private val authenticationFacade: AuthenticationFacade,
) : TranslationSuggestionService {
    override fun getKeysWithSuggestions(
        projectId: Long,
        keyIds: List<Long>,
        languageIds: List<Long>
    ): Map<Long, List<TranslationSuggestionView>> {
        val data = translationSuggestionRepository.getByKeyId(projectId, languageIds, keyIds,)
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
        projectId: Long,
        keyId: Long,
        languageId: Long,
        dto: CreateTranslationSuggestionRequest,
    ): TranslationSuggestion {
        val key = keyService.find(keyId) ?: throw NotFoundException(Message.KEY_NOT_FOUND)
        keyService.checkInProject(key, projectId)

        val language =
            languageService.findEntity(languageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

        val suggestion = TranslationSuggestion(
            key = key,
            project = entityManager.getReference(Project::class.java, projectId),
            language = language,
            author = authenticationFacade.authenticatedUserEntity,
            translation = dto.translation,
        )

        translationSuggestionRepository.save(suggestion)
        return suggestion
    }

    fun getSuggestionsPaged(
        pageable: Pageable, projectId: Long, languageId: Long, keyId: Long, filters: SuggestionFilters,
    ): Page<TranslationSuggestion> {
        return translationSuggestionRepository.getPaged(
            pageable,
            projectId,
            languageId,
            keyId,
        )
    }
}
