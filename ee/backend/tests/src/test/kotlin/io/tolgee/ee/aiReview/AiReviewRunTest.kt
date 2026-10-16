package io.tolgee.ee.aiReview

import io.tolgee.batch.BatchJobCancellationManager
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.batch.data.BatchJobType
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.development.testDataBuilder.data.AiReviewRunTestData
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.service.aiReview.AiReviewTriggerService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.waitFor
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.repository.activity.ActivityRevisionRepository
import io.tolgee.testing.assert
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AiReviewRunTest : AbstractAiReviewTest() {
  @Autowired
  lateinit var activityRevisionRepository: ActivityRevisionRepository

  @Autowired
  lateinit var triggerService: AiReviewTriggerService

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  lateinit var batchJobCancellationManager: BatchJobCancellationManager

  override val enabledFeatures = setOf(Feature.AI_REVIEW, Feature.QA_CHECKS)

  override fun createTestData() = AiReviewRunTestData()

  private val data get() = testData as AiReviewRunTestData

  private val allKeyIds get() = data.allKeys.map { it.self.id }

  private val previewUrl get() = "/v2/projects/$projectId/ai-review/run/preview"
  private val runUrl get() = "/v2/projects/$projectId/ai-review/run"
  private val backlogUrl get() = "/v2/projects/$projectId/ai-review/rules/backlog"

  @Test
  fun `previews what a run reviews and why the rest is skipped`() {
    saveTestData()
    val total =
      listOf(
        data.reviewedWithoutRereview,
        data.reviewedOnlyWithRereview,
        data.humanReviewed,
        data.withoutRule,
        data.never,
        data.withOpenQaIssues,
      ).sumOf { it.size }
    performAuthPost(previewUrl, AiReviewRunRequest(keyIds = allKeyIds)).andIsOk.andAssertThatJson {
      node("total").isEqualTo(total)
      node("withoutRereview.willReview").isEqualTo(data.reviewedWithoutRereview.size)
      node("withoutRereview.dryRunOnly").isEqualTo(data.dryRunOnly.size)
      node("withoutRereview.alreadyHasResult").isEqualTo(data.reviewedOnlyWithRereview.size)
      node("withoutRereview.humanReviewed").isEqualTo(data.humanReviewed.size)
      node("withoutRereview.noRule").isEqualTo(data.withoutRule.size)
      node("withoutRereview.never").isEqualTo(data.never.size)
      node("withoutRereview.openQaIssues").isEqualTo(data.withOpenQaIssues.size)
      node("withRereview.willReview").isEqualTo(
        data.reviewedWithoutRereview.size + data.reviewedOnlyWithRereview.size,
      )
      node("withRereview.alreadyHasResult").isEqualTo(0)
      node("withRereview.humanReviewed").isEqualTo(data.humanReviewed.size)
    }
  }

  @Test
  fun `preview is limited to the selected non-base languages`() {
    saveTestData()
    performAuthPost(
      previewUrl,
      AiReviewRunRequest(keyIds = allKeyIds, languageIds = listOf(data.czech.self.id, data.englishLanguage.id)),
    ).andIsOk.andAssertThatJson {
      node("total").isEqualTo(1)
      node("withoutRereview.never").isEqualTo(1)
      node("withoutRereview.willReview").isEqualTo(0)
    }
  }

  @Test
  fun `run reviews exactly the counted translations through the normal pipeline`() {
    saveTestData()
    performAuthPost(runUrl, AiReviewRunRequest(keyIds = allKeyIds)).andIsOk.andAssertThatJson {
      node("type").isEqualTo("AI_REVIEW")
      node("totalItems").isEqualTo(4)
      node("author.id").isEqualTo(data.user.id)
    }
    waitForProjectJobsCompleted()

    val job = aiReviewJobs().single()
    job.hidden.assert.isFalse()
    data.uiGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.uiGerman
      .state()
      .assert
      .isEqualTo(TranslationState.REVIEWED)
    data.failedGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.staleGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.legalGerman.review().outcomeIs(AiReviewOutcome.WOULD_AUTO_REVIEW)
    data.legalGerman
      .state()
      .assert
      .isEqualTo(TranslationState.TRANSLATED)
    data.belowGerman.review().outcomeIs(AiReviewOutcome.BELOW_THRESHOLD)
    data.approvedGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.confirmedGerman.review().outcomeIs(AiReviewOutcome.CONFIRMED)
    data.qaGerman
      .review()
      .assert
      .isNull()
    data.uiCzech
      .review()
      .assert
      .isNull()
    data.uiFrench
      .review()
      .assert
      .isNull()
    data.rejectedGerman.review().outcomeIs(AiReviewOutcome.REJECTED)

    executeInNewTransaction(platformTransactionManager) {
      activityRevisionRepository
        .findAll()
        .filter { it.batchJob?.id == job.id }
        .map { it.authorId }
        .assert
        .isNotEmpty
        .containsOnlyNulls()
    }
  }

  @Test
  fun `re-review demotes an AI approval that now scores below the threshold`() {
    saveTestData()
    performAuthPost(
      runUrl,
      AiReviewRunRequest(keyIds = listOf(data.approvedKey.self.id, data.belowKey.self.id), rereview = true),
    ).andIsOk
    waitForProjectJobsCompleted()

    val approved = data.approvedGerman.review()!!
    approved.outcome.assert.isEqualTo(AiReviewOutcome.BELOW_THRESHOLD)
    approved.score.assert.isEqualTo(75)
    approved.previousOutcome.assert.isNull()
    data.approvedGerman
      .state()
      .assert
      .isEqualTo(TranslationState.TRANSLATED)

    data.belowGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.belowGerman
      .state()
      .assert
      .isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `failed re-review keeps the earlier AI approval`() {
    saveTestData()
    performAuthPost(runUrl, AiReviewRunRequest(keyIds = listOf(data.approvedInvalidKey.self.id), rereview = true))
      .andIsOk
    waitForProjectJobsCompleted()

    val approved = data.approvedInvalidGerman.review()!!
    approved.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    approved.score.assert.isEqualTo(90)
    approved.previousOutcome.assert.isNull()
    data.approvedInvalidGerman
      .state()
      .assert
      .isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `un-reviewing a queued re-review rejects the AI approval`() {
    saveTestData()
    executeInNewTransaction(platformTransactionManager) {
      triggerService.upsertPendingForRun(
        listOf(
          AiReviewTriggerService.RunTarget(
            translationId = data.approvedGerman.self.id,
            textHash = aiReviewTextHash(data.approvedGerman.self.text),
            rereviewOfAutoReviewed = true,
          ),
        ),
        runBatchJobId = 0,
      )
    }
    data.approvedGerman.review().outcomeIs(AiReviewOutcome.PENDING)

    performAuthPut("/v2/projects/$projectId/translations/${data.approvedGerman.self.id}/set-state/TRANSLATED", null)
      .andIsOk
    waitForProjectJobsCompleted()

    data.approvedGerman.review().outcomeIs(AiReviewOutcome.REJECTED)
  }

  @Test
  fun `rule scoped run covers the whole project and only the rule's translations`() {
    saveTestData()
    performAuthPost(previewUrl, AiReviewRunRequest(ruleId = data.legalRule.self.id)).andIsOk.andAssertThatJson {
      node("total").isEqualTo(1)
      node("withoutRereview.willReview").isEqualTo(1)
      node("withoutRereview.dryRunOnly").isEqualTo(1)
    }
    performAuthPost(runUrl, AiReviewRunRequest(ruleId = data.legalRule.self.id)).andIsOk.andAssertThatJson {
      node("totalItems").isEqualTo(1)
    }
    waitForProjectJobsCompleted()
    data.legalGerman.review().outcomeIs(AiReviewOutcome.WOULD_AUTO_REVIEW)
    data.uiGerman
      .review()
      .assert
      .isNull()
  }

  @Test
  fun `returns the backlog of every enabled rule`() {
    saveTestData()
    assertBacklogs(uiRule = listOf(data.uiGerman, data.staleGerman), legalRule = listOf(data.legalGerman))
  }

  @Test
  fun `rule scoped run keeps to the rule's languages and the default branch`() {
    data.limitLegalRuleToGermanOnDefaultBranch()
    saveTestData()
    performAuthPost(previewUrl, AiReviewRunRequest(ruleId = data.legalRule.self.id)).andIsOk.andAssertThatJson {
      node("total").isEqualTo(1)
      node("withoutRereview.willReview").isEqualTo(1)
    }
    performAuthPost(runUrl, AiReviewRunRequest(ruleId = data.legalRule.self.id)).andIsOk.andAssertThatJson {
      node("totalItems").isEqualTo(1)
    }
    waitForProjectJobsCompleted()
    data.legalGerman.review().outcomeIs(AiReviewOutcome.WOULD_AUTO_REVIEW)
    data.legalFrench
      .review()
      .assert
      .isNull()
    data.branchedLegalGerman
      .review()
      .assert
      .isNull()
  }

  @Test
  fun `rule run skips translations whose branch the rule excludes`() {
    data.limitLegalRuleToGermanOnDefaultBranch()
    data.legalRule.self.branchNotIn = mutableListOf("ma*")
    saveTestData()
    performAuthPost(previewUrl, AiReviewRunRequest(ruleId = data.legalRule.self.id)).andIsOk.andAssertThatJson {
      node("withoutRereview.willReview").isEqualTo(0)
    }
  }

  @Test
  fun `queued translations are counted, reviewed once and stay out of the backlog`() {
    data.addQueuedReviews()
    saveTestData()
    val queuedKeyIds = listOf(data.pendingKey.self.id, data.queuedRereviewKey.self.id)
    performAuthPost(previewUrl, AiReviewRunRequest(keyIds = queuedKeyIds)).andIsOk.andAssertThatJson {
      node("total").isEqualTo(2)
      node("withoutRereview.willReview").isEqualTo(2)
      node("withoutRereview.humanReviewed").isEqualTo(0)
    }
    assertBacklogs(uiRule = listOf(data.uiGerman, data.staleGerman), legalRule = listOf(data.legalGerman))

    performAuthPost(runUrl, AiReviewRunRequest(keyIds = queuedKeyIds)).andIsOk.andAssertThatJson {
      node("totalItems").isEqualTo(2)
    }
    waitForProjectJobsCompleted()

    data.pendingGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    val rereviewed = data.queuedRereviewGerman.review()!!
    rereviewed.outcome.assert.isEqualTo(AiReviewOutcome.AUTO_REVIEWED)
    rereviewed.previousOutcome.assert.isNull()
    data.queuedRereviewGerman
      .state()
      .assert
      .isEqualTo(TranslationState.REVIEWED)
  }

  @Test
  fun `backlog leaves out translations a running run has queued`() {
    saveTestData()
    data.failedGerman.review().outcomeIs(AiReviewOutcome.FAILED)
    assertBacklogs(uiRule = listOf(data.uiGerman, data.staleGerman), legalRule = listOf(data.legalGerman))

    whileJobsPaused {
      performAuthPost(runUrl, AiReviewRunRequest(keyIds = listOf(data.uiKey.self.id))).andIsOk
      data.uiGerman.review().outcomeIs(AiReviewOutcome.PENDING)
      assertBacklogs(uiRule = listOf(data.staleGerman), legalRule = listOf(data.legalGerman))
    }
    waitForProjectJobsCompleted()
  }

  @Test
  fun `backlog counts only languages where the caller can change the state`() {
    saveTestData()
    userAccount = data.germanRuleRunner
    assertBacklogs(uiRule = listOf(data.uiGerman, data.staleGerman), legalRule = listOf(data.legalGerman))

    userAccount = data.czechRuleRunner
    assertBacklogs(uiRule = emptyList(), legalRule = emptyList())
  }

  @Test
  fun `aborting a run restores what it had not reached and leaves rows it did not queue`() {
    data.addQueuedReviews()
    saveTestData()
    val keyIds = allKeyIds + listOf(data.pendingKey.self.id, data.queuedRereviewKey.self.id)
    whileJobsPaused {
      performAuthPost(runUrl, AiReviewRunRequest(keyIds = keyIds, rereview = true)).andIsOk
      listOf(
        data.uiGerman,
        data.failedGerman,
        data.belowGerman,
        data.dryRunBelowGerman,
        data.approvedInvalidGerman,
        data.staleGerman,
      ).forEach { it.review().outcomeIs(AiReviewOutcome.PENDING) }
      claim(data.staleGerman)
      batchJobCancellationManager.cancel(aiReviewJobs().single().id)
    }
    waitForProjectJobsCompleted()

    aiReviewJobs()
      .single { !it.hidden }
      .status.assert
      .isEqualTo(BatchJobStatus.CANCELLED)
    data.uiGerman
      .review()
      .assert
      .isNull()
    data.legalGerman
      .review()
      .assert
      .isNull()
    data.failedGerman.review().outcomeIs(AiReviewOutcome.FAILED)
    data.belowGerman.review().let {
      it.outcomeIs(AiReviewOutcome.BELOW_THRESHOLD)
      it!!.score.assert.isEqualTo(70)
      it.rule
        ?.id.assert
        .isEqualTo(data.uiRule.self.id)
    }
    data.dryRunBelowGerman.review().let {
      it.outcomeIs(AiReviewOutcome.BELOW_THRESHOLD)
      it!!.dryRun.assert.isTrue()
      it.score.assert.isEqualTo(70)
    }
    data.approvedInvalidGerman.review().let {
      it.outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
      it!!.score.assert.isEqualTo(90)
    }
    data.staleGerman.review().outcomeIs(AiReviewOutcome.PENDING)
    data.pendingGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.queuedRereviewGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
  }

  @Test
  fun `aborting a run hands translations another running run also targets over to that run`() {
    saveTestData()
    whileJobsPaused {
      val firstRunId =
        performAuthPost(runUrl, AiReviewRunRequest(keyIds = listOf(data.uiKey.self.id))).getIdFromResponse()
      val secondRunId =
        performAuthPost(runUrl, AiReviewRunRequest(keyIds = listOf(data.uiKey.self.id, data.staleKey.self.id)))
          .getIdFromResponse()
      data.uiGerman
        .review()!!
        .runBatchJobId.assert
        .isEqualTo(firstRunId)

      batchJobCancellationManager.cancel(firstRunId)
      waitFor { aiReviewJobs().single { it.id == firstRunId }.status == BatchJobStatus.CANCELLED }
      waitFor { data.uiGerman.review()?.runBatchJobId == secondRunId }
      data.uiGerman.review().outcomeIs(AiReviewOutcome.PENDING)
    }
    waitForProjectJobsCompleted()

    data.uiGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
    data.staleGerman.review().outcomeIs(AiReviewOutcome.AUTO_REVIEWED)
  }

  @Test
  fun `fails when there is nothing to review`() {
    saveTestData()
    performAuthPost(runUrl, AiReviewRunRequest(keyIds = listOf(data.plainKey.self.id)))
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    jobsOfType(BatchJobType.AI_REVIEW).assert.isEmpty()
  }

  @Test
  fun `requires keys or a rule`() {
    saveTestData()
    performAuthPost(previewUrl, AiReviewRunRequest())
      .andIsBadRequest
      .andHasErrorMessage(Message.AI_REVIEW_RUN_SCOPE_REQUIRED)
  }

  @Test
  fun `requires the state change permission for every language`() {
    saveTestData()
    userAccount = data.translator
    performAuthPost(previewUrl, AiReviewRunRequest(keyIds = allKeyIds)).andIsForbidden
    performAuthPost(runUrl, AiReviewRunRequest(keyIds = allKeyIds)).andIsForbidden

    userAccount = data.czechReviewer
    performAuthPost(
      runUrl,
      AiReviewRunRequest(keyIds = allKeyIds, languageIds = listOf(data.german.self.id)),
    ).andIsForbidden
  }

  @Test
  fun `is hidden when the flag is off and needs the plan feature`() {
    saveTestData()
    tolgeeProperties.featureFlags.enabledForAll = emptyList()
    performAuthPost(previewUrl, AiReviewRunRequest(keyIds = allKeyIds)).andIsNotFound
    performAuthPost(runUrl, AiReviewRunRequest(keyIds = allKeyIds)).andIsNotFound
    performAuthGet(backlogUrl).andIsNotFound

    tolgeeProperties.featureFlags.enabledForAll = listOf(FeatureFlag.AI_REVIEW)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.QA_CHECKS)
    performAuthPost(runUrl, AiReviewRunRequest(keyIds = allKeyIds))
      .andIsBadRequest
      .andHasErrorMessage(Message.FEATURE_NOT_ENABLED)
  }

  private fun whileJobsPaused(fn: () -> Unit) {
    batchJobConcurrentLauncher.pause = true
    try {
      fn()
    } finally {
      batchJobConcurrentLauncher.pause = false
    }
  }

  private fun claim(translation: TranslationBuilder) {
    executeInNewTransaction(platformTransactionManager) {
      val review = translationAiReviewRepository.findByTranslationId(translation.self.id)!!
      review.claimedAt = currentDateProvider.date
      translationAiReviewRepository.save(review)
    }
  }

  private fun assertBacklogs(
    uiRule: List<TranslationBuilder>,
    legalRule: List<TranslationBuilder>,
  ) {
    performAuthGet(backlogUrl).andIsOk.andAssertThatJson {
      node("backlogs").isEqualTo(
        """
        [
          {"ruleId": ${data.uiRule.self.id}, "notReviewed": ${uiRule.size}},
          {"ruleId": ${data.legalRule.self.id}, "notReviewed": ${legalRule.size}}
        ]
        """.trimIndent(),
      )
    }
  }

  private fun TranslationBuilder.review(): TranslationAiReview? = findReview(self.id)

  private fun TranslationBuilder.state(): TranslationState = getTranslation(self.id).state

  private fun TranslationAiReview?.outcomeIs(outcome: AiReviewOutcome) {
    this.assert.isNotNull
    this!!.outcome.assert.isEqualTo(outcome)
  }
}
