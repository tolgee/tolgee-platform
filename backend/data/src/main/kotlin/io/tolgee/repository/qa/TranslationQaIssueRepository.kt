package io.tolgee.repository.qa

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
