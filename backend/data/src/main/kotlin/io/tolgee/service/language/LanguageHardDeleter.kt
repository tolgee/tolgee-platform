package io.tolgee.service.language

import io.tolgee.model.Language
import io.tolgee.model.translation.Translation
import io.tolgee.repository.LanguageRepository
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
    languageWithData.translations = allTranslations
    languageRepository.delete(language)
    entityManager.flush()
  }

  private fun getAllTranslations(languageWithData: Language) =
    languageWithData.translations.chunked(30000).flatMap {
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
    }.toMutableList()

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
}
