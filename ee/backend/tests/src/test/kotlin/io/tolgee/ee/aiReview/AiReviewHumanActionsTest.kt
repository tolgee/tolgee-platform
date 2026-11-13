package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.SetTranslationsStateStateRequest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.development.AiReviewTestData
import io.tolgee.ee.service.aiReview.AiReviewActionService
import io.tolgee.ee.service.aiReview.AiReviewDemotionServiceEeImpl
import io.tolgee.ee.service.aiReview.AiReviewEligibility
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
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
class AiReviewHumanActionsTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var batchJobService: BatchJobService

  @Autowired
  private lateinit var translationAiReviewRepository: TranslationAiReviewRepository

  @Autowired
  private lateinit var demotionService: AiReviewDemotionServiceEeImpl

  @Autowired
  private lateinit var aiReviewActionService: AiReviewActionService

  lateinit var testData: AiReviewTestData
  lateinit var reviewer: UserAccount

  private val projectUrl get() = "/v2/projects/${testData.project.id}"
  private val aiReviewUrl get() = "$projectUrl/translations/${testData.deTranslation.id}/ai-review"

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.AI_REVIEW)
    testData = AiReviewTestData()
    testData.deTranslation.state = TranslationState.REVIEWED
    testData.deTranslation.auto = true
    testData.addReview(testData.deTranslationBuilder, AiReviewStatus.APPROVED, 88) {
      rule = testData.rule
      threshold = 85
      promptName = "Default review prompt"
      providerName = "server-provider"
      model = "gpt-test"
    }
    reviewer =
      testData.addUserWithScopes(
        "ai_review_state_editor",
        Scope.KEYS_VIEW,
        Scope.TRANSLATIONS_VIEW,
        Scope.TRANSLATIONS_EDIT,
        Scope.TRANSLATIONS_STATE_EDIT,
      )
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

  @Test
  fun `un-review with the set-state endpoint marks OVERRIDDEN and resolves the row`() {
    testData.deTranslationBuilder.data.aiReviews
      .single()
      .self
      .apply {
        lastFailureReason = AiReviewFailureReason.PROVIDER_ERROR
        lastFailedAt = currentDateProvider.date
        reReviewRequested = true
      }
    save()
    performAuthPut(
      "$projectUrl/translations/${testData.deTranslation.id}/set-state/TRANSLATED",
      null,
    ).andIsOk

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    row.reReviewRequested.assert.isFalse()
    row.lastFailureReason.assert.isNull()
    row.lastFailedAt.assert.isNull()
    translation(testData.deTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `reviewing over a below-threshold result overrides it`() {
    testData.frTranslation.state = TranslationState.TRANSLATED
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.BELOW_THRESHOLD, 40) {
      rule = testData.rule
    }
    save()

    performAuthPut(
      "$projectUrl/translations/${testData.frTranslation.id}/set-state/REVIEWED",
      null,
    ).andIsOk

    val row = review(testData.frTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    translation(testData.frTranslation.id).state.assert.isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `reviewing over a score-only result overrides it`() {
    testData.rule.mode = AiReviewRuleMode.SCORE_ONLY
    testData.frTranslation.state = TranslationState.TRANSLATED
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.SCORE_ONLY_FAIL, 40) {
      rule = testData.rule
    }
    save()

    performAuthPut(
      "$projectUrl/translations/${testData.frTranslation.id}/set-state/REVIEWED",
      null,
    ).andIsOk

    val row = review(testData.frTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
  }

  @Test
  fun `retry is rejected for an overridden row`() {
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.OVERRIDDEN, 40) {
      resolvedBy = testData.user
    }
    save()

    performAuthPost(
      "$projectUrl/translations/${testData.frTranslation.id}/ai-review/retry",
      null,
    ).andIsBadRequest.andHasErrorMessage(Message.AI_REVIEW_NOTHING_TO_REVIEW)

    review(testData.frTranslation.id)!!.reReviewRequested.assert.isFalse()
  }

  @Test
  fun `un-review without the feature deletes the approval and keeps no marker`() {
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()
    performAuthPut(
      "$projectUrl/translations/${testData.deTranslation.id}/set-state/TRANSLATED",
      null,
    ).andIsOk

    review(testData.deTranslation.id).assert.isNull()
  }

  @Test
  fun `un-review with the project switch off deletes the approval and keeps no marker`() {
    testData.project.useAiReview = false
    save()
    performAuthPut(
      "$projectUrl/translations/${testData.deTranslation.id}/set-state/TRANSLATED",
      null,
    ).andIsOk

    review(testData.deTranslation.id).assert.isNull()
  }

  @Test
  fun `a machine overwrite that un-reviews an AI approval deletes the row instead of overriding`() {
    save()

    aiReviewActionService.settleHumanUnReview(
      projectId = testData.project.id,
      translationIds = listOf(testData.deTranslation.id),
      textChangedIds = setOf(testData.deTranslation.id),
      resolvedById = testData.user.id,
      aiReviewOn = true,
    )

    review(testData.deTranslation.id).assert.isNull()
  }

  @Test
  fun `a bulk un-review without the feature deletes a FAILED marker`() {
    testData.frTranslation.state = TranslationState.REVIEWED
    testData.frTranslation.auto = true
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.FAILED)
    save()
    enabledFeaturesProvider.forceEnabled = emptySet()

    bulkSetFrenchState(TranslationState.TRANSLATED)

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      review(testData.frTranslation.id).assert.isNull()
    }
  }

  @Test
  fun `a batch un-review overrides an AI approval and keeps the machine-translated flag`() {
    save()
    bulkSetState(testData.german.id, TranslationState.TRANSLATED)

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      review(testData.deTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    }
    val row = review(testData.deTranslation.id)!!
    row.resolvedBy!!
      .id.assert
      .isEqualTo(testData.user.id)
    translation(testData.deTranslation.id).auto.assert.isTrue()
  }

  @Test
  fun `a bulk un-review deletes a FAILED marker so the text is not shown as failed`() {
    testData.frTranslation.state = TranslationState.REVIEWED
    testData.frTranslation.auto = true
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.FAILED)
    save()

    bulkSetFrenchState(TranslationState.TRANSLATED)

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      review(testData.frTranslation.id).assert.isNull()
    }
    translation(testData.frTranslation.id).state.assert.isEqualTo(TranslationState.TRANSLATED)
  }

  @Test
  fun `a bulk human review of an MT text with an AI row overrides the row`() {
    saveFrenchMtTextWithBelowThresholdRow()
    bulkReviewFrench()

    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      review(testData.frTranslation.id)?.status.assert.isEqualTo(AiReviewStatus.OVERRIDDEN)
    }
    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.get(testData.frTranslation.id)
      translation.state.assert.isEqualTo(TranslationState.REVIEWED)
      AiReviewEligibility
        .isHumanReviewed(translation, translationAiReviewRepository.findByTranslationId(translation.id))
        .assert
        .isTrue()
    }
  }

  @Test
  fun `a bulk human review while AI review is off still makes it a human review`() {
    saveFrenchMtTextWithBelowThresholdRow()
    enabledFeaturesProvider.forceEnabled = emptySet()
    bulkReviewFrench()

    assertFrenchIsHumanReviewed()
  }

  private fun saveFrenchMtTextWithBelowThresholdRow() {
    testData.frTranslation.auto = true
    testData.addReview(testData.frTranslationBuilder, AiReviewStatus.BELOW_THRESHOLD, 40) {
      rule = testData.rule
    }
    save()
  }

  private fun bulkReviewFrench() = bulkSetState(testData.french.id, TranslationState.REVIEWED)

  private fun bulkSetFrenchState(state: TranslationState) = bulkSetState(testData.french.id, state)

  private fun bulkSetState(
    languageId: Long,
    state: TranslationState,
  ) {
    val job =
      executeInNewTransaction(platformTransactionManager) {
        batchJobService.startJob(
          request =
            SetTranslationsStateStateRequest().apply {
              keyIds = listOf(testData.key.id)
              languageIds = listOf(languageId)
              this.state = state
            },
          project = entityManager.getReference(Project::class.java, testData.project.id),
          author = entityManager.getReference(UserAccount::class.java, testData.user.id),
          type = BatchJobType.SET_TRANSLATIONS_STATE,
          isHidden = false,
        )
      }
    waitForNotThrowing(pollTime = 100, timeout = 30_000) {
      batchJobService
        .getJobDtoNoCache(job.id)
        .status.assert
        .isEqualTo(BatchJobStatus.SUCCESS)
    }
  }

  private fun assertFrenchIsHumanReviewed() {
    waitForNotThrowing(pollTime = 100, timeout = 10_000) {
      review(testData.frTranslation.id).assert.isNull()
    }
    executeInNewTransaction(platformTransactionManager) {
      val translation = translationService.get(testData.frTranslation.id)
      translation.state.assert.isEqualTo(TranslationState.REVIEWED)
      AiReviewEligibility.isHumanReviewed(translation, null).assert.isTrue()
    }
  }

  @Test
  fun `demotion by the system does not mark OVERRIDDEN`() {
    save()
    executeInNewTransaction(platformTransactionManager) {
      entityManager.find(AiReviewRule::class.java, testData.rule.id).threshold = 95
    }
    executeInNewTransaction(platformTransactionManager) {
      demotionService.demoteKeys(testData.project.id, listOf(testData.key.id))
    }

    val row = review(testData.deTranslation.id)!!
    row.status.assert.isEqualTo(AiReviewStatus.BELOW_THRESHOLD)
    row.resolvedBy.assert.isNull()
  }

  @Test
  fun `GET returns the details and ruleName is null after the rule is deleted`() {
    save()
    performAuthGet(aiReviewUrl).andIsOk.andAssertThatJson {
      node("status").isEqualTo("APPROVED")
      node("score").isEqualTo(88)
      node("threshold").isEqualTo(85)
      node("ruleId").isEqualTo(testData.rule.id)
      node("ruleName").isEqualTo("UI")
      node("promptName").isEqualTo("Default review prompt")
      node("providerName").isEqualTo("server-provider")
      node("model").isEqualTo("gpt-test")
      node("scoreOnlyRun").isEqualTo(false)
      node("resolvedBy").isNull()
    }

    performAuthDelete("$projectUrl/ai-review-rules/${testData.rule.id}").andIsOk
    performAuthGet(aiReviewUrl).andIsOk.andAssertThatJson {
      node("status").isEqualTo("APPROVED")
      node("ruleId").isNull()
      node("ruleName").isNull()
    }
  }

  private fun review(translationId: Long): TranslationAiReview? =
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.findByTranslationId(translationId)?.also { it.resolvedBy?.id }
    }

  private fun translation(id: Long) =
    executeInNewTransaction(platformTransactionManager) {
      translationService.get(id)
    }
}
