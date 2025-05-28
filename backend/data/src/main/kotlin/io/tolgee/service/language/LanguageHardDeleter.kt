package io.tolgee.service.language

import io.tolgee.model.Language
import io.tolgee.model.task.Task
import io.tolgee.model.translation.Translation
import io.tolgee.repository.LanguageRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.task.ITaskService
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

/**
 * This class helps to delete language without n+1 problems.
 *
 * It fetches all the relations first, so they're not fetched later when
 * required for orphan removal or activity storing.
 *
 * It's tested by `io.tolgee.service.LanguageServiceTest.hard deletes language without n+1s`
 */
class LanguageHardDeleter(
  private val language: Language,
  private val applicationContext: ApplicationContext,
) {
  fun delete() {
    val languageWithData = getWithFetchedTranslations(language)
    val allTranslations = getAllTranslations(languageWithData)
    val tasks = getAllTasks(languageWithData)
    translationRepository.deleteAll(allTranslations)
    taskService.deleteAll(tasks)
    languageRepository.delete(languageWithData)
    aiPlaygroundResultService.deleteResultsByLanguage(language.id)
    entityManager.flush()
  }

  private fun getAllTranslations(languageWithData: Language) =
    languageWithData.translations.chunked(30000).flatMap {
      val withComments =
        entityManager.createQuery(
          """from Translation t
            join fetch t.key k
            left join fetch k.keyMeta km
            left join fetch k.namespace
            left join fetch t.comments
            where t.id in :ids""",
          Translation::class.java,
        )
          .setParameter("ids", it.map { it.id })
          .resultList

      withComments
    }.toMutableList()

  fun getAllTasks(languageWithData: Language) =
    entityManager.createQuery(
      """from Task tk
            join fetch tk.keys
            where tk.language = :languageWithData""",
      Task::class.java,
    )
      .setParameter("languageWithData", languageWithData)
      .resultList
      .toMutableList()

  private fun getWithFetchedTranslations(language: Language): Language {
    return entityManager.createQuery(
      """
          from Language l
          left join fetch l.translations t
          where l = :language""",
      Language::class.java,
    )
      .setParameter("language", language)
      .singleResult
  }

  private val entityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }

  private val languageRepository by lazy {
    applicationContext.getBean(LanguageRepository::class.java)
  }

  private val translationRepository by lazy {
    applicationContext.getBean(TranslationRepository::class.java)
  }

  private val taskService by lazy {
    applicationContext.getBean(ITaskService::class.java)
  }

  private val aiPlaygroundResultService by lazy {
    applicationContext.getBean(AiPlaygroundResultService::class.java)
  }
}
