package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.AiReviewRule
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AiReviewRuleRepository : JpaRepository<AiReviewRule, Long> {
  fun findAllByProjectId(projectId: Long): List<AiReviewRule>

  fun findByProjectIdAndId(
    projectId: Long,
    id: Long,
  ): AiReviewRule?

  @Query(
    """
    select distinct r from AiReviewRule r
    left join fetch r.languages
    left join fetch r.overrides o
    left join fetch o.language
    where r.project.id = :projectId and r.enabled = true
    """,
  )
  fun findEnabledWithScope(projectId: Long): List<AiReviewRule>
}
