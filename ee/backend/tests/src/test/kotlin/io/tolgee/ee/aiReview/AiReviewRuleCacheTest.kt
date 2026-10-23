package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleCache
import io.tolgee.ee.service.aiReview.AiReviewRuleEvaluator
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
  ],
)
class AiReviewRuleCacheTest : AbstractSpringTest() {
  @Autowired
  lateinit var evaluator: AiReviewRuleEvaluator

  @Autowired
  lateinit var aiReviewRuleCache: AiReviewRuleCache

  @Autowired
  lateinit var aiReviewRuleRepository: AiReviewRuleRepository

  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    testData = AiReviewTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `rules are cached until the cache is evicted`() {
    threshold().assert.isEqualTo(85)

    executeInNewTransaction(platformTransactionManager) {
      val rule = aiReviewRuleRepository.findById(testData.rule.id).get()
      rule.threshold = 99
      aiReviewRuleRepository.save(rule)
    }
    threshold().assert.isEqualTo(85)

    aiReviewRuleCache.evict(testData.project.id)
    threshold().assert.isEqualTo(99)
  }

  private fun threshold(): Int =
    executeInNewTransaction(platformTransactionManager) {
      val decision = evaluator.evaluate(translationService.get(testData.deTranslation.id)) as RuleDecision.Review
      decision.threshold
    }
}
