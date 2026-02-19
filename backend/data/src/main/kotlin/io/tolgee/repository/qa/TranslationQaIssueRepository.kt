package io.tolgee.repository.qa

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
  fun findAllByProjectAndTranslation(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue>
}
