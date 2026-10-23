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
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
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
  fun `counts follow the statuses`() {
    review("k.approved1", AiReviewStatus.APPROVED, reviewed = true)
    review("k.approved2", AiReviewStatus.APPROVED, reviewed = true)
    review("k.confirmed", AiReviewStatus.CONFIRMED, reviewed = true)
    review("k.rejected", AiReviewStatus.REJECTED)
    review("k.failed", AiReviewStatus.FAILED)
    review("k.dryPass", AiReviewStatus.DRY_RUN_PASS)
    review("k.dryFail", AiReviewStatus.DRY_RUN_FAIL)
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.rules[0].counts.autoReviewed").isEqualTo(4)
      node("_embedded.rules[0].counts.rejected").isEqualTo(1)
      node("_embedded.rules[0].counts.failed").isEqualTo(1)
      node("_embedded.rules[0].counts.scored").isEqualTo(2)
      node("_embedded.rules[0].counts.wouldBeAutoReviewed").isEqualTo(1)
    }
  }

  @Test
  fun `an approval with a failed re-review counts as failed`() {
    review("k.approved", AiReviewStatus.APPROVED, reviewed = true) {
      lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
    }
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.rules[0].counts.autoReviewed").isEqualTo(1)
      node("_embedded.rules[0].counts.failed").isEqualTo(1)
    }
  }

  @Test
  fun `rows with a stale hash are not counted`() {
    review("k.stale", AiReviewStatus.APPROVED, reviewed = true) { textHash = "old" }
    save()

    performAuthGet(rulesUrl).andIsOk.andAssertThatJson {
      node("_embedded.rules[0].counts.autoReviewed").isEqualTo(0)
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
    testData.addRule {
      name = "Checkout"
      tagIn = mutableListOf("checkout")
      threshold = 92
    }
    save()

    // UI wins for: checkout.pay (de, fr), k.plain (de, fr), k.outdated (fr), k.humanReviewed (fr),
    // k.hasRow (fr), k.staleRow (de, fr) — the stale row does not count as a result
    val counts = statsService.getCounts(testData.project.id)
    counts
      .getValue(testData.rule.id)
      .backlog.assert
      .isEqualTo(9)
    counts.values
      .single { it.ruleId != testData.rule.id }
      .backlog.assert
      .isEqualTo(2)
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

    val counts = statsService.getCounts(testData.project.id)
    // the feature rule covers only the key on feature/x, the other rule the key on the default branch
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
  fun `POST response contains the backlog count`() {
    save()
    performAuthPost(rulesUrl, mapOf("tagIn" to listOf("ui"), "threshold" to 95)).andIsOk.andAssertThatJson {
      node("counts.backlog").isEqualTo(2)
      node("counts.autoReviewed").isEqualTo(0)
    }
  }
}
