package io.tolgee.repository.qa

import io.tolgee.dtos.queryResults.qa.LanguageQaIssueCount
import io.tolgee.dtos.queryResults.qa.QaIssueCountByCheckType
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface TranslationQaIssueRepository : JpaRepository<TranslationQaIssue, Long> {
  @Query(
    """
    select t.language.id as languageId, count(i) as count
    from TranslationQaIssue i
    join i.translation t
    join t.key k
    where k.project.id = :projectId
    and i.state = io.tolgee.model.enums.qa.QaIssueState.OPEN
    group by t.language.id
    """,
  )
  fun getOpenIssueCountsByLanguageId(projectId: Long): List<LanguageQaIssueCount>

  @Query(
    """
    select t.language.id as languageId, count(t) as count
    from Translation t
    join t.key k
    where k.project.id = :projectId
    and t.qaChecksStale = true
    group by t.language.id
    """,
  )
  fun getStaleCountsByLanguageId(projectId: Long): List<LanguageQaIssueCount>

  @Query(
    """
    select i.type as checkType, count(i) as count
    from TranslationQaIssue i
    join i.translation t
    join t.key k
    where k.project.id = :projectId
    and t.language.id = :languageId
    and i.state = io.tolgee.model.enums.qa.QaIssueState.OPEN
    group by i.type
    """,
  )
  fun getOpenIssueCountsByCheckType(
    projectId: Long,
    languageId: Long,
  ): List<QaIssueCountByCheckType>

  @Query(
    """
    select i from TranslationQaIssue i
    where i.translation.id in :translationIds
    """,
  )
  fun findByTranslationIds(translationIds: List<Long>): List<TranslationQaIssue>

  fun findAllByTranslationId(translationId: Long): List<TranslationQaIssue>

  @Modifying
  @Query("delete from TranslationQaIssue i where i.translation.id = :translationId")
  fun deleteAllByTranslationId(translationId: Long)

  @Query(
    """
    select i from TranslationQaIssue i
    join i.translation t
    join t.key k
    where k.project.id = :projectId
    and t.id = :translationId
    """,
  )
  fun findAllByProjectIdAndTranslationId(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue>

  @Query(
    """
    select i from TranslationQaIssue i
    join i.translation t
    join t.key k
    where k.project.id = :projectId
    and t.id = :translationId
    and i.id = :issueId
    """,
  )
  fun findByProjectIdAndTranslationIdAndId(
    projectId: Long,
    translationId: Long,
    issueId: Long,
  ): TranslationQaIssue?

  @Query(
    """
    select i from TranslationQaIssue i
    join i.translation t
    join t.key k
    where k.project.id = :projectId
    and i.translation.id = :translationId
    and i.type = :type
    and i.message = :message
    and (i.replacement = :replacement or (i.replacement is null and :replacement is null))
    and (i.positionStart = :positionStart or (i.positionStart is null and :positionStart is null))
    and (i.positionEnd = :positionEnd or (i.positionEnd is null and :positionEnd is null))
    and (i.pluralVariant = :pluralVariant or (i.pluralVariant is null and :pluralVariant is null))
    """,
  )
  fun findByProjectIdAndTranslationIdAndIssueParams(
    projectId: Long,
    translationId: Long,
    type: QaCheckType,
    message: QaIssueMessage,
    replacement: String?,
    positionStart: Int?,
    positionEnd: Int?,
    pluralVariant: String?,
  ): TranslationQaIssue?
}
