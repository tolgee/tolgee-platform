package io.tolgee.ee.aiReview

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewRuleStatsService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class AiReviewRuleStatsTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var statsService: AiReviewRuleStatsService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  lateinit var testData: AiReviewTestData

  private val rulesUrl get() = "/v2/projects/${testData.project.id}/ai-review-rules"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW, Feature.BRANCHING)
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
    userAccount = null
    enabledFeaturesProvider.forceEnabled = null
  }

  private fun save() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  private fun review(
    name: String,
    status: AiReviewStatus,
    reviewed: Boolean = false,
    ft: TranslationAiReview.() -> Unit = {},
  ): TranslationBuilder {
    val de = testData.addKeyWithTranslations(name).de
    if (reviewed) de.self.state = TranslationState.REVIEWED
    testData.addReview(de, status, 90) {
      rule = testData.rule
      ft()
    }
    return de
  }

  @Test
  fun `counts follow the statuses and the rule counters`() {
    testData.rule.autoReviewedCount = 4
    testData.rule.rejectedCount = 1
    review("k.approved1", AiReviewStatus.APPROVED, reviewed = true)
    review("k.failed", AiReviewStatus.FAILED) { failureReason = AiReviewFailureReason.PROVIDER_ERROR }
    review("k.dryPass", AiReviewStatus.DRY_RUN_PASS)
    review("k.dryFail", AiReviewStatus.DRY_RUN_FAIL)
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules[0].counts.autoReviewed").isEqualTo(4)
      node("_embedded.aiReviewRules[0].counts.rejected").isEqualTo(1)
      node("_embedded.aiReviewRules[0].counts.failed").isEqualTo(1)
      node("_embedded.aiReviewRules[0].counts.scored").isEqualTo(2)
      node("_embedded.aiReviewRules[0].counts.wouldBeAutoReviewed").isEqualTo(1)
    }
  }

  @Test
  fun `an approval with a failed re-review counts as failed`() {
    review("k.approved", AiReviewStatus.APPROVED, reviewed = true) {
      lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules[0].counts.failed").isEqualTo(1)
    }
  }

  @Test
  fun `a FAILED marker without a failure reason is not counted and not retried`() {
    val marker = review("k.marker", AiReviewStatus.FAILED, reviewed = true)
    marker.self.auto = true
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules[0].counts.failed").isEqualTo(0)
    }
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findFailedTranslationIdsByRule(testData.rule.id)
    }.assert.isEmpty()
  }

  @Test
  fun `rows with a stale hash are not counted`() {
    review("k.stale", AiReviewStatus.FAILED) {
      textHash = "old"
      failureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules[0].counts.failed").isEqualTo(0)
    }
  }

  @Test
  fun `a rejection still counts after the text is edited`() {
    val de = review("k.approved", AiReviewStatus.APPROVED, reviewed = true)
    de.self.auto = true
    save()

    performAuthPut("/v2/projects/${testData.project.id}/translations/${de.self.id}/ai-review/reject", null).andIsOk
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations",
      mapOf("key" to "k.approved", "translations" to mapOf("de" to "Korrigierter Text")),
    ).andIsOk

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules[0].counts.rejected").isEqualTo(1)
    }
  }

  @Test
  fun `backlog counts eligible translations without a current row where the rule wins`() {
    testData.addKeyWithTranslations("k.plain")
    testData.addKeyWithTranslations("k.checkout", tags = listOf("ui", "checkout"))
    testData
      .addKeyWithTranslations("k.outdated")
      .de.self.outdated = true
    testData
      .addKeyWithTranslations("k.humanReviewed")
      .de.self.state = TranslationState.REVIEWED
    review("k.hasRow", AiReviewStatus.BELOW_THRESHOLD)
    review("k.staleRow", AiReviewStatus.BELOW_THRESHOLD) { textHash = "old" }
    val checkoutRule =
      testData.addRule {
        name = "Checkout"
        tagIn = mutableListOf("checkout")
        threshold = 92
      }
    save()

    backlogOf(testData.rule.id).assert.containsExactlyInAnyOrder(
      "checkout.pay/de",
      "checkout.pay/fr",
      "k.plain/de",
      "k.plain/fr",
      "k.outdated/fr",
      "k.humanReviewed/fr",
      "k.hasRow/fr",
      "k.staleRow/de",
      "k.staleRow/fr",
    )
    backlogOf(checkoutRule.self.id).assert.containsExactlyInAnyOrder("k.checkout/de", "k.checkout/fr")
    val counts = statsService.getCounts(testData.project.id)
    counts
      .getValue(testData.rule.id)
      .backlog.assert
      .isEqualTo(9)
    counts
      .getValue(checkoutRule.self.id)
      .backlog.assert
      .isEqualTo(2)
  }

  private fun backlogOf(ruleId: Long): Set<String> {
    val ids = mutableListOf<Long>()
    statsService.forEachBacklogItem(testData.project.id, ruleId) { ids.add(it) }
    return executeInNewTransaction(platformTransactionManager) {
      ids.map { translationService.get(it) }.map { "${it.key.name}/${it.language.tag}" }.toSet()
    }
  }

  @Test
  fun `backlog sums all branches the rule covers`() {
    testData.project.useBranching = true
    testData.projectBuilder.addBranch {
      name = "main"
      isDefault = true
    }
    val featureBranch = testData.projectBuilder.addBranch { name = "feature/x" }.self
    testData.addKeyWithTranslations("k.onFeature").key.branch = featureBranch
    val featureRule =
      testData.addRule {
        branchIn = mutableListOf("feature/*")
        threshold = 99
      }
    testData.rule.tagIn = mutableListOf()
    save()

    backlogOf(featureRule.self.id).assert.containsExactlyInAnyOrder("k.onFeature/de", "k.onFeature/fr")
    backlogOf(testData.rule.id).assert.containsExactlyInAnyOrder("checkout.pay/de", "checkout.pay/fr")
    val counts = statsService.getCounts(testData.project.id)
    counts
      .getValue(featureRule.self.id)
      .backlog.assert
      .isEqualTo(2)
    counts
      .getValue(testData.rule.id)
      .backlog.assert
      .isEqualTo(2)
  }

  @Test
  fun `forEachBacklogItem visits the counted translations`() {
    testData.addKeyWithTranslations("k.plain")
    save()

    val ids = mutableListOf<Long>()
    statsService.forEachBacklogItem(testData.project.id, testData.rule.id) { ids.add(it) }
    ids.size.assert.isEqualTo(4)
    ids.assert.contains(testData.deTranslation.id, testData.frTranslation.id)
  }

  @Test
  fun `the POST response has no counts and the list has them`() {
    save()
    performAuthPost(rulesUrl, mapOf("tagIn" to listOf("ui"), "threshold" to 95)).andIsOk.andAssertThatJson {
      node("counts").isNull()
    }
    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.aiReviewRules[1].counts.backlog").isEqualTo(2)
      node("_embedded.aiReviewRules[1].counts.autoReviewed").isEqualTo(0)
    }
  }
}
