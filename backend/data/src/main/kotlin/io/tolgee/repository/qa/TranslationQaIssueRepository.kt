package io.tolgee.repository.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.qa.TranslationQaIssue
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface LanguageQaIssueCount {
  val languageId: Long
  val count: Long
}

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
    select i from TranslationQaIssue i
    where i.translation.id in :translationIds
    and i.state = io.tolgee.model.enums.qa.QaIssueState.OPEN
    """,
  )
  fun findOpenByTranslationIds(translationIds: List<Long>): List<TranslationQaIssue>

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
    and i.id = :issueId
    """,
  )
  fun findByProjectIdAndId(
    projectId: Long,
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
    and i.replacement = :replacement
    and i.positionStart = :positionStart
    and i.positionEnd = :positionEnd
    """,
  )
  fun findByProjectIdAndTranslationIdAndIssueParams(
    projectId: Long,
    translationId: Long,
    type: QaCheckType,
    message: QaIssueMessage,
    replacement: String?,
    positionStart: Int,
    positionEnd: Int,
  ): TranslationQaIssue?
}
