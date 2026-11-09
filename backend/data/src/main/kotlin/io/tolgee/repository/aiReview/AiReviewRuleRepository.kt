package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AiReviewRuleRepository : JpaRepository<AiReviewRule, Long> {
  fun findAllByProjectIdOrderById(projectId: Long): List<AiReviewRule>

  @Query(
    """
    select distinct r from AiReviewRule r
    left join fetch r.languages
    left join fetch r.overrides o
    left join fetch o.language
    left join fetch r.prompt
    where r.project.id = :projectId
    order by r.id
    """,
  )
  fun findAllWithScopeByProjectId(projectId: Long): List<AiReviewRule>

  @Query(
    """
    select r from AiReviewRule r
    left join fetch r.languages
    left join fetch r.overrides o
    left join fetch o.language
    left join fetch r.prompt
    where r.project.id = :projectId and r.id = :id
    """,
  )
  fun findWithScopeByProjectIdAndId(
    projectId: Long,
    id: Long,
  ): AiReviewRule?

  fun findAllByProjectIdAndPromptId(
    projectId: Long,
    promptId: Long,
  ): List<AiReviewRule>

  fun countByProjectIdAndPromptIdAndMode(
    projectId: Long,
    promptId: Long,
    mode: AiReviewRuleMode,
  ): Long

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

  @Query(
    """
    select r.id as ruleId, r.autoReviewedCount as autoReviewedCount, r.rejectedCount as rejectedCount
    from AiReviewRule r
    where r.project.id = :projectId
    order by r.id
    """,
  )
  fun findDecisionCounts(projectId: Long): List<RuleDecisionCountView>

  @Modifying
  @Query(
    nativeQuery = true,
    value = "update ai_review_rule set auto_reviewed_count = auto_reviewed_count + :count where id = :ruleId",
  )
  fun addToAutoReviewedCount(
    ruleId: Long,
    count: Long,
  ): Int

  @Modifying
  @Query(
    nativeQuery = true,
    value = "update ai_review_rule set rejected_count = rejected_count + :count where id = :ruleId",
  )
  fun addToRejectedCount(
    ruleId: Long,
    count: Long,
  ): Int

  interface RuleDecisionCountView {
    val ruleId: Long
    val autoReviewedCount: Long
    val rejectedCount: Long
  }
}
