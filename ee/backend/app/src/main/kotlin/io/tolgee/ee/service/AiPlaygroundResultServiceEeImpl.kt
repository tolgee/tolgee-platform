package io.tolgee.ee.service

import io.tolgee.ee.repository.AiPlaygroundResultRepository
import io.tolgee.model.AiPlaygroundResult
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiPlaygroundResultServiceEeImpl(
  private val aiPlaygroundResultRepository: AiPlaygroundResultRepository,
  private val projectService: ProjectService,
  private val userAccountService: UserAccountService,
  private val keyService: KeyService,
  private val languageService: LanguageService,
  private val entityManager: EntityManager,
) : AiPlaygroundResultService {
  fun getResult(
    projectId: Long,
    userId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Collection<AiPlaygroundResult> {
    return aiPlaygroundResultRepository.getResults(projectId, userId, keyIds, languageIds)
  }

  @Transactional
  override fun setResult(
    projectId: Long,
    userId: Long,
    keyId: Long,
    languageId: Long,
    translation: String?,
    contextDescription: String?,
  ) {
    val project = projectService.get(projectId)
    val key = keyService.get(keyId)
    val language = languageService.getEntity(languageId, projectId)
    val user = userAccountService.get(userId)
    val result =
      AiPlaygroundResult(
        project,
        key,
        language,
        user,
        translation = translation,
        contextDescription = contextDescription,
      )

    aiPlaygroundResultRepository.save(result)
  }

  @Transactional
  override fun removeResults(
    projectId: Long,
    userId: Long,
  ) {
    aiPlaygroundResultRepository.removeResults(projectId, userId)
  }

  override fun deleteResultsByLanguage(languageId: Long) {
    aiPlaygroundResultRepository.deleteAiPlaygroundResultsByLanguage(languageId)
  }

  override fun deleteResultsByProject(projectId: Long) {
    aiPlaygroundResultRepository.deleteAiPlaygroundResultsByProject(projectId)
  }

  override fun deleteResultsByUser(userId: Long) {
    aiPlaygroundResultRepository.deleteAiPlaygroundResultsByUser(userId)
  }

  override fun deleteResultsByKeys(keys: Collection<Long>) {
    aiPlaygroundResultRepository.deleteAiPlaygroundResultsByKeys(keys)
  }
}
