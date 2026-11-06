package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewEligibility
import io.tolgee.ee.service.aiReview.AiReviewRuleEvaluator
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AiReviewRuleEvaluatorTest : AbstractSpringTest() {
  @Autowired
  lateinit var evaluator: AiReviewRuleEvaluator

  @Autowired
  lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  lateinit var testData: AiReviewTestData

  @BeforeEach
  fun setup() {
    testData = AiReviewTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `evaluate picks the strictest of two DB rules`() {
    val stricter =
      testData.addRule {
        tagIn = mutableListOf("ui")
        threshold = 92
        mode = AiReviewRuleMode.ACTIVE
      }
    testDataService.saveTestData(testData.root)

    val decision = evaluate(testData.deTranslation.id) as RuleDecision.Review
    decision.rule.id.assert
      .isEqualTo(stricter.self.id)
    decision.threshold.assert.isEqualTo(92)
  }

  @Test
  fun `evaluate ignores disabled rules`() {
    testData.addRule {
      tagIn = mutableListOf("ui")
      threshold = 92
      enabled = false
    }
    testDataService.saveTestData(testData.root)

    (evaluate(testData.deTranslation.id) as RuleDecision.Review)
      .rule.id.assert
      .isEqualTo(testData.rule.id)
  }

  @Test
  fun `evaluate with activeOnly skips Dry run rules`() {
    testData.addRule {
      tagIn = mutableListOf("ui")
      threshold = 95
      mode = AiReviewRuleMode.DRY_RUN
    }
    testDataService.saveTestData(testData.root)

    val decision = evaluate(testData.deTranslation.id, activeOnly = true) as RuleDecision.Review
    decision.rule.id.assert
      .isEqualTo(testData.rule.id)
  }

  @Test
  fun `forEachDecision visits every non-base key-language pair once`() {
    testData.addKeyWithTranslations("checkout.cancel")
    testDataService.saveTestData(testData.root)

    val visited = mutableListOf<Pair<Long, Long>>()
    evaluator.forEachDecision(testData.project.id, null, null) { keyId, languageId, _ ->
      visited.add(keyId to languageId)
    }
    visited.size.assert.isEqualTo(4)
    visited
      .toSet()
      .size.assert
      .isEqualTo(4)
    visited
      .map { it.second }
      .toSet()
      .assert
      .containsExactlyInAnyOrder(testData.german.id, testData.french.id)
  }

  @Test
  fun `forEachDecision respects keyIds and languageIds filters`() {
    testData.addKeyWithTranslations("checkout.cancel")
    testDataService.saveTestData(testData.root)

    val visited = mutableListOf<Pair<Long, Long>>()
    evaluator.forEachDecision(testData.project.id, listOf(testData.key.id), listOf(testData.french.id)) { k, l, _ ->
      visited.add(k to l)
    }
    visited.assert.containsExactly(testData.key.id to testData.french.id)
  }

  @Test
  fun `countDecisions groups keys with the same scope`() {
    testData.addKeyWithTranslations("checkout.cancel")
    testData.addKeyWithTranslations("checkout.back")
    testData.addKeyWithTranslations("legal.terms", tags = listOf("legal"))
    testDataService.saveTestData(testData.root)

    val counts = evaluator.countDecisions(testData.project.id, null, listOf(testData.german.id))
    val reviewCount = counts.filter { it.decision is RuleDecision.Review }.sumOf { it.keyCount }
    val noRuleCount = counts.filter { it.decision == RuleDecision.NoRule }.sumOf { it.keyCount }
    reviewCount.assert.isEqualTo(3)
    noRuleCount.assert.isEqualTo(1)
    counts
      .single { it.decision is RuleDecision.Review }
      .keyCount.assert
      .isEqualTo(3)

    var perKeyReviews = 0L
    evaluator.forEachDecision(testData.project.id, null, listOf(testData.german.id)) { _, _, decision ->
      if (decision is RuleDecision.Review) perKeyReviews++
    }
    perKeyReviews.assert.isEqualTo(reviewCount)
  }

  @Test
  fun `translations that are not eligible`() {
    val base = testData.englishTranslationBuilder
    val empty = testData.addKeyWithTranslations("k.empty", deText = "").de
    val blank = testData.addKeyWithTranslations("k.blank", deText = "   ").de
    val disabled =
      testData.addKeyWithTranslations("k.disabled", deText = "").de.also {
        it.self.state =
          TranslationState.DISABLED
      }
    val outdated = testData.addKeyWithTranslations("k.outdated").de.also { it.self.outdated = true }
    val humanReviewed =
      testData.addKeyWithTranslations("k.reviewed").de.also { it.self.state = TranslationState.REVIEWED }
    val confirmed =
      testData.addKeyWithTranslations("k.confirmed").de.also {
        it.self.state = TranslationState.REVIEWED
        testData.addReview(it, AiReviewStatus.CONFIRMED, 90)
      }
    val staleApproval =
      testData.addKeyWithTranslations("k.stale").de.also {
        it.self.state = TranslationState.REVIEWED
        testData.addReview(it, AiReviewStatus.APPROVED, 90) { textHash = "old" }
      }
    val bulkReviewedMachineTranslation =
      testData.addKeyWithTranslations("k.bulk").de.also {
        it.self.state = TranslationState.REVIEWED
        it.self.auto = true
      }
    val bulkReviewedAfterDryRun =
      testData.addKeyWithTranslations("k.bulk.dry").de.also {
        it.self.state = TranslationState.REVIEWED
        it.self.auto = true
        testData.addReview(it, AiReviewStatus.DRY_RUN_PASS, 90)
      }
    testDataService.saveTestData(testData.root)

    listOf(
      base,
      empty,
      blank,
      disabled,
      outdated,
      humanReviewed,
      confirmed,
      staleApproval,
      bulkReviewedMachineTranslation,
      bulkReviewedAfterDryRun,
    ).forEach {
      isEligible(it.self.id).assert.describedAs(it.self.text).isFalse()
    }
  }

  @Test
  fun `translations that are eligible`() {
    val approved =
      testData.addKeyWithTranslations("k.approved").de.also {
        it.self.state = TranslationState.REVIEWED
        testData.addReview(it, AiReviewStatus.APPROVED, 90)
      }
    testDataService.saveTestData(testData.root)

    isEligible(testData.deTranslation.id).assert.isTrue()
    isEligible(approved.self.id).assert.isTrue()
    isAiApproval(approved.self.id).assert.isTrue()
  }

  @Test
  fun `reviewed machine translation the AI still manages is not human-reviewed`() {
    val approvalOverwrittenByMt =
      aiManagedMachineTranslation("k.mt.over", AiReviewStatus.APPROVED) { textHash = "old" }
    val reReviewQueued = aiManagedMachineTranslation("k.mt.queued", AiReviewStatus.PENDING)
    val reReviewFailedUnderProtection = aiManagedMachineTranslation("k.mt.failed", AiReviewStatus.BELOW_THRESHOLD)
    testDataService.saveTestData(testData.root)

    listOf(approvalOverwrittenByMt, reReviewQueued, reReviewFailedUnderProtection).forEach {
      isHumanReviewed(it.self.id).assert.describedAs(it.self.key.name).isFalse()
      isAiApproval(it.self.id).assert.describedAs(it.self.key.name).isFalse()
      isEligible(it.self.id).assert.describedAs(it.self.key.name).isTrue()
    }
  }

  private fun aiManagedMachineTranslation(
    keyName: String,
    status: AiReviewStatus,
    ft: TranslationAiReview.() -> Unit = {},
  ) = testData.addKeyWithTranslations(keyName).de.also {
    it.self.state = TranslationState.REVIEWED
    it.self.auto = true
    testData.addReview(it, status, 60, ft)
  }

  @Test
  fun `stale approval is not an AI approval`() {
    val stale =
      testData.addKeyWithTranslations("k.stale").de.also {
        it.self.state = TranslationState.REVIEWED
        testData.addReview(it, AiReviewStatus.APPROVED, 90) { textHash = "old" }
      }
    testDataService.saveTestData(testData.root)

    isAiApproval(stale.self.id).assert.isFalse()
  }

  private fun evaluate(
    translationId: Long,
    activeOnly: Boolean = false,
  ): RuleDecision =
    executeInNewTransaction(platformTransactionManager) {
      evaluator.evaluate(loadTranslation(translationId), activeOnly)
    }

  private fun isEligible(translationId: Long): Boolean =
    executeInNewTransaction(platformTransactionManager) {
      val translation = loadTranslation(translationId)
      AiReviewEligibility.isEligible(
        translation,
        testData.englishLanguage.id,
        translationAiReviewRepository.findByTranslationId(translationId),
      )
    }

  private fun isHumanReviewed(translationId: Long): Boolean =
    executeInNewTransaction(platformTransactionManager) {
      AiReviewEligibility.isHumanReviewed(
        loadTranslation(translationId),
        translationAiReviewRepository.findByTranslationId(translationId),
      )
    }

  private fun isAiApproval(translationId: Long): Boolean =
    executeInNewTransaction(platformTransactionManager) {
      AiReviewEligibility.isAiApproval(
        loadTranslation(translationId),
        translationAiReviewRepository.findByTranslationId(translationId),
      )
    }

  private fun loadTranslation(id: Long): Translation = translationService.get(id)
}
