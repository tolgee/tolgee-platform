package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Caches
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class AiReviewRuleCache(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val cacheManager: CacheManager,
) {
  @Cacheable(cacheNames = [Caches.AI_REVIEW_RULES], key = "#projectId")
  @Transactional(readOnly = true)
  fun get(projectId: Long): List<RuleSnapshot> =
    ArrayList(aiReviewRuleRepository.findEnabledWithScope(projectId).map { it.toSnapshot() })

  fun evict(projectId: Long) {
    evictNow(projectId)
    evictAgainAfterCompletion(projectId)
  }

  // Between the in-transaction evict and the commit, a concurrent reader can put the old rules back.
  private fun evictAgainAfterCompletion(projectId: Long) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return
    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun afterCompletion(status: Int) {
          evictNow(projectId)
        }
      },
    )
  }

  private fun evictNow(projectId: Long) {
    cacheManager.getCache(Caches.AI_REVIEW_RULES)?.evict(projectId)
  }
}
