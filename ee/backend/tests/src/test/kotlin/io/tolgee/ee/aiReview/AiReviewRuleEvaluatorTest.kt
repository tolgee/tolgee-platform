package io.tolgee.ee.aiReview

import io.tolgee.AbstractSpringTest
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewEligibility
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
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
      testData.addKeyWithTranslations("k.overridden").de.also {
        it.self.state = TranslationState.REVIEWED
        testData.addReview(it, AiReviewStatus.OVERRIDDEN, 90)
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
    val bulkReviewedAfterScoreOnlyRun =
      testData.addKeyWithTranslations("k.bulk.dry").de.also {
        it.self.state = TranslationState.REVIEWED
        it.self.auto = true
        testData.addReview(it, AiReviewStatus.SCORE_ONLY_PASS, 90)
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
      bulkReviewedAfterScoreOnlyRun,
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
