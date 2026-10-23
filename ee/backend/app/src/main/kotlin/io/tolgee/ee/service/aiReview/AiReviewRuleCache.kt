package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Caches
import io.tolgee.ee.service.aiReview.matching.EffectiveValue
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AiReviewRuleCache(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
) {
  @Cacheable(cacheNames = [Caches.AI_REVIEW_RULES], key = "#projectId")
  @Transactional(readOnly = true)
  fun get(projectId: Long): List<RuleSnapshot> =
    ArrayList(aiReviewRuleRepository.findEnabledWithScope(projectId).map { it.toSnapshot() })

  @CacheEvict(cacheNames = [Caches.AI_REVIEW_RULES], key = "#projectId")
  fun evict(projectId: Long) {
  }

  private fun AiReviewRule.toSnapshot() =
    RuleSnapshot(
      id = id,
      enabled = enabled,
      tagIn = tagIn.toSet(),
      tagNotIn = tagNotIn.toSet(),
      namespaceIn = namespaceIn.toSet(),
      namespaceNotIn = namespaceNotIn.toSet(),
      languageIds = languages.map { it.id }.toSet(),
      branchIn = branchIn.toList(),
      branchNotIn = branchNotIn.toList(),
      value = EffectiveValue(threshold = threshold, never = never),
      overrides = overrides.associate { it.language.id to EffectiveValue(it.threshold, it.never) },
      mode = mode,
      promptId = prompt?.id,
      skipOpenQaIssues = skipOpenQaIssues,
    )
}
