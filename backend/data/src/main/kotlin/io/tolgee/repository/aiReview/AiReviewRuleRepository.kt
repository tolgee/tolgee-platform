package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.AiReviewRule
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AiReviewRuleRepository : JpaRepository<AiReviewRule, Long> {
  fun findAllByProjectId(projectId: Long): List<AiReviewRule>

  fun findByProjectIdAndId(
    projectId: Long,
    id: Long,
  ): AiReviewRule?
}
