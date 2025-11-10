package io.tolgee.ee.repository

import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.views.TranslationSuggestionView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
        ts.translation as translation,
        ts.state as state,
        ts.is_plural as plural,
        
        u.id as authorId,
        u.name as authorName,
        u.username as authorUsername,
        u.avatar_hash as authorAvatarHash,
        u.deleted_at as authorDeletedAt
      from translation_suggestion ts
        left join language l on l.id = ts.language_id
        left join user_account u on u.id = ts.author_id
      where ts.key_id in :keyIds
        and ts.project_id = :projectId
        and ts.language_id in :languageIds
        and ts.state = 'ACTIVE'
      order by ts.language_id, ts.key_id, ts.created_at DESC
    """,
    nativeQuery = true,
  )
  fun getByKeyId(
    projectId: Long,
    languageIds: List<Long>,
    keyIds: List<Long>,
  ): List<TranslationSuggestionView>

  @Query(
    """
      from TranslationSuggestion ts
        left join fetch ts.language
        left join fetch ts.author
      where ts.project.id = :projectId
        and ts.key.id = :keyId
        and ts.language.id = :languageId
        and (
            :#{#filters.filterState} is null
            or ts.state in :#{#filters.filterState}
        )
    """,
  )
  fun getPaged(
    pageable: Pageable,
    projectId: Long,
    languageId: Long,
    keyId: Long,
    filters: SuggestionFilters,
  ): Page<TranslationSuggestion>

  @Query(
    """
        from TranslationSuggestion ts
        where ts.project.id = :projectId
            and ts.language.id = :languageId
            and ts.key.id = :keyId
            and ts.state = 'ACTIVE'
    """,
  )
  fun getAllActive(
    projectId: Long,
    languageId: Long,
    keyId: Long,
  ): List<TranslationSuggestion>

  @Query(
    """
        from TranslationSuggestion ts
            join ts.language
            join ts.key
        where ts.project.id = :projectId
            and ts.language.id = :languageId
            and ts.key.id = :keyId
            and ts.translation = :translation
            and ts.isPlural = :isPlural
            and ts.state = 'ACTIVE'
    """,
  )
  fun findSuggestion(
    projectId: Long,
    languageId: Long,
    keyId: Long,
    translation: String,
    isPlural: Boolean,
  ): List<TranslationSuggestion>

  @Query(
    """
      from TranslationSuggestion ts
      where ts.language.id = :id
    """,
  )
  fun getAllByLanguage(id: Long): List<TranslationSuggestion>

  @Query(
    """
      from TranslationSuggestion ts
      where ts.project.id = :id
    """,
  )
  fun getAllByProject(id: Long): List<TranslationSuggestion>
}
