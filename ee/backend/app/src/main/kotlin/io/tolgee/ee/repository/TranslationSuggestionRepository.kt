package io.tolgee.ee.repository

import io.tolgee.model.TranslationSuggestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TranslationSuggestionRepository : JpaRepository<TranslationSuggestion, Long> {
  @Query("""
    from TranslationSuggestion ts
    where
        ts.key.id in :keyIds
        and ts.project.id = :projectId
  """)
  fun getByKeyId(projectId: Long, keyIds: List<Long>): List<TranslationSuggestion>
}
