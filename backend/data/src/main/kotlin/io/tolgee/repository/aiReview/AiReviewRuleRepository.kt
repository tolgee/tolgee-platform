package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface AiReviewRuleRepository : JpaRepository<AiReviewRule, Long> {
  @Query(
    """
    from AiReviewRule r
    left join fetch r.prompt
    where r.project.id = :projectId
    order by r.id
    """,
  )
  fun findAllByProjectId(projectId: Long): List<AiReviewRule>

  @Query(
    """
    from AiReviewRule r
    left join fetch r.prompt
    where r.project.id = :projectId and r.enabled = true
    order by r.id
    """,
  )
  fun findAllEnabledByProjectId(projectId: Long): List<AiReviewRule>

  fun existsByProjectIdAndEnabledTrue(projectId: Long): Boolean

  @Query(
    """
    from AiReviewRule r
    left join fetch r.prompt
    where r.project.id = :projectId and r.id = :ruleId
    """,
  )
  fun findByProjectIdAndId(
    projectId: Long,
    ruleId: Long,
  ): AiReviewRule?

  @Transactional
  @Modifying
  @Query(
    """
    update AiReviewRule r
    set r.mode = :dryRun
    where r.prompt.id = :promptId and r.mode = :active
    """,
  )
  fun setDryRunByPromptId(
    promptId: Long,
    dryRun: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN,
    active: AiReviewRuleMode = AiReviewRuleMode.ACTIVE,
  ): Int

  @Transactional
  @Modifying
  @Query(
    """
    update AiReviewRule r
    set r.prompt = null, r.mode = :dryRun
    where r.prompt.id = :promptId
    """,
  )
  fun detachPromptAndSetDryRun(
    promptId: Long,
    dryRun: AiReviewRuleMode = AiReviewRuleMode.DRY_RUN,
  ): Int
}
