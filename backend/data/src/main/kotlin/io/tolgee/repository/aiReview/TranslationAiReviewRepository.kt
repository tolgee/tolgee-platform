package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface TranslationAiReviewRepository : JpaRepository<TranslationAiReview, Long> {
  fun findByTranslationId(translationId: Long): TranslationAiReview?

  @Query(
    nativeQuery = true,
    value = """
    select r.rule_id as ruleId, r.status as status, (r.last_failure_reason is not null) as lastFailed,
           count(*) as count
    from translation_ai_review r
    join translation t on t.id = r.translation_id
    join key k on k.id = t.key_id
    where k.project_id = :projectId
      and r.rule_id is not null
      and r.text_hash = encode(sha256(convert_to(coalesce(t.text, ''), 'UTF8')), 'hex')
    group by r.rule_id, r.status, (r.last_failure_reason is not null)
    """,
  )
  fun countByRuleAndStatus(projectId: Long): List<RuleStatusCountView>

  interface RuleStatusCountView {
    val ruleId: Long
    val status: String
    val lastFailed: Boolean
    val count: Long
  }
}
