package io.tolgee.service.translation

import io.tolgee.model.views.TranslationSuggestionView
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class TranslationSuggestionServiceOssImpl(
  protected val entityManager: EntityManager,
) : TranslationSuggestionService {
  override fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Map<Pair<Long, String>, List<TranslationSuggestionView>> {
    return mutableMapOf()
  }

  @Transactional
  override fun deleteAllByLanguage(id: Long) {
    entityManager
      .createQuery("delete from TranslationSuggestion ts where ts.language.id = :id")
      .setParameter("id", id)
      .executeUpdate()
  }

  @Transactional
  override fun deleteAllByProject(id: Long) {
    entityManager
      .createQuery("delete from TranslationSuggestion ts where ts.project.id = :id")
      .setParameter("id", id)
      .executeUpdate()
  }

  @Transactional
  override fun deleteAllByKeyIds(keyIds: Collection<Long>) {
    keyIds.chunked(IN_CLAUSE_BATCH_SIZE).forEach { chunk ->
      entityManager
        .createQuery("delete from TranslationSuggestion ts where ts.key.id in :keyIds")
        .setParameter("keyIds", chunk)
        .executeUpdate()
    }
  }

  companion object {
    // Keep the "in :keyIds" bind list well under PostgreSQL's 65535 prepared-statement parameter limit.
    private const val IN_CLAUSE_BATCH_SIZE = 30_000
  }
}
