package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.dtos.request.key.UpdateNamespaceDto
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleCache
import io.tolgee.ee.service.aiReview.AiReviewRuleEvaluator
import io.tolgee.ee.service.aiReview.AiReviewRuleService
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

  @Autowired
  lateinit var aiReviewRuleService: AiReviewRuleService

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

  @Test
  fun `create, update, enable and delete evict the rule cache`() {
    threshold().assert.isEqualTo(85)
    val projectId = testData.project.id

    val created = aiReviewRuleService.create(projectId, AiReviewRuleRequest(tagIn = listOf("ui"), threshold = 90))
    threshold().assert.isEqualTo(90)

    aiReviewRuleService.update(projectId, created.id, AiReviewRuleRequest(tagIn = listOf("ui"), threshold = 93))
    threshold().assert.isEqualTo(93)

    aiReviewRuleService.setEnabled(projectId, created.id, false)
    threshold().assert.isEqualTo(85)

    aiReviewRuleService.setEnabled(projectId, created.id, true)
    threshold().assert.isEqualTo(93)

    aiReviewRuleService.delete(projectId, created.id)
    threshold().assert.isEqualTo(85)
  }

  @Test
  fun `language delete and namespace rename evict the rule cache`() {
    executeInNewTransaction(platformTransactionManager) {
      val rule = aiReviewRuleRepository.findById(testData.rule.id).get()
      rule.languages = mutableSetOf(languageService.getEntity(testData.german.id))
      rule.namespaceIn = mutableListOf("")
      aiReviewRuleRepository.save(rule)
    }
    aiReviewRuleCache.evict(testData.project.id)
    rules()
      .single()
      .languageIds.assert
      .containsExactly(testData.german.id)

    executeInNewTransaction(platformTransactionManager) { languageService.deleteLanguage(testData.german.id) }
    rules().assert.isEmpty()

    executeInNewTransaction(platformTransactionManager) {
      aiReviewRuleService.setEnabled(testData.project.id, testData.rule.id, true)
    }
    executeInNewTransaction(platformTransactionManager) {
      val namespace = namespaceService.create("checkout", testData.project.id)!!
      namespaceService.update(namespace, UpdateNamespaceDto(name = "cart"))
    }
    executeInNewTransaction(platformTransactionManager) {
      val rule = aiReviewRuleRepository.findById(testData.rule.id).get()
      rule.namespaceIn = mutableListOf("checkout")
      aiReviewRuleRepository.save(rule)
    }
    aiReviewRuleCache.evict(testData.project.id)
    executeInNewTransaction(platformTransactionManager) {
      val namespace = namespaceService.find("cart", testData.project.id)!!
      namespaceService.update(namespace, UpdateNamespaceDto(name = "checkout"))
    }
    rules()
      .single()
      .namespaceIn.assert
      .containsExactly("checkout")
  }

  private fun rules() = evaluator.loadRules(testData.project.id)

  private fun threshold(): Int =
    executeInNewTransaction(platformTransactionManager) {
      val decision = evaluator.evaluate(translationService.get(testData.deTranslation.id)) as RuleDecision.Review
      decision.threshold
    }
}
