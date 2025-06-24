package io.tolgee.ee.repository

import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.views.TranslationSuggestionView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TranslationSuggestionRepository : JpaRepository<TranslationSuggestion, Long> {
  @Query(
    """
      select distinct on (ts.language_id, ts.key_id)
        ts.id as id,
        ts.key_id as keyId,
        ts.language_id as languageId,
        l.tag as languageTag,
        ts.user_id as userId,
        ts.translation as translation,
        ts.state as state
      from translation_suggestion ts
        left join language l on l.id = ts.language_id
      where ts.key_id in :keyIds
        and ts.project_id = :projectId
        and ts.language_id in :languageIds
    """,
  nativeQuery = true
  )
  fun getByKeyId(projectId: Long, keyIds: List<Long>, languageIds: List<Long>): List<TranslationSuggestionView>
}
