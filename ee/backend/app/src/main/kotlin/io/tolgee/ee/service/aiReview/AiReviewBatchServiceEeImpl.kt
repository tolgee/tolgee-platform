package io.tolgee.ee.service.aiReview

import io.tolgee.activity.ActivityHolder
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.aiReview.scoring.AiReviewScoring
import io.tolgee.ee.service.aiReview.scoring.ReviewParseException
import io.tolgee.ee.service.prompt.DefaultReviewPromptHelper
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.exceptions.LlmProviderNotFoundException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.Project
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.PromptRepository
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.aiReview.AiReviewBatchService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.util.executeInNewTransaction
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

@Primary
@Service
class AiReviewBatchServiceEeImpl(
  private val evaluator: AiReviewRuleEvaluator,
  private val aiReviewPromptService: AiReviewPromptService,
  private val reviewRepository: TranslationAiReviewRepository,
  private val ruleRepository: AiReviewRuleRepository,
  private val promptRepository: PromptRepository,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val qaGate: AiReviewQaGate,
  private val websocketPublisher: AiReviewWebsocketPublisher,
  private val currentDateProvider: CurrentDateProvider,
  private val transactionManager: PlatformTransactionManager,
  private val entityManager: EntityManager,
  private val activityHolder: ActivityHolder,
) : AiReviewBatchService {
  private data class Assignment(
    val translation: Translation,
    val projectId: Long,
    val organizationId: Long,
    val baseLanguageId: Long?,
    val textHash: String,
    val ruleId: Long,
    val ruleMode: AiReviewRuleMode,
    val promptId: Long?,
    val promptName: String,
    val promptRevision: Int?,
    val aiManaged: Boolean,
    val requeuedHumanDecision: Boolean,
  )

  private enum class QaGateResult { PASS, WAIT_FOR_FRESH_QA, OPEN_ISSUES }

  private sealed interface LlmOutcome {
    data class Success(
      val issues: List<AiReviewIssue>,
      val score: Int,
      val providerName: String?,
      val model: String?,
    ) : LlmOutcome

    data class Failure(
      val reason: AiReviewFailureReason,
    ) : LlmOutcome
  }

  override fun reviewTranslation(
    job: BatchJobDto,
    translationId: Long,
  ) {
    // Read in the chunk transaction: the inner transactions below have their own ActivityHolder.
    val chunkExecutionId = activityHolder.activityRevision.batchJobChunkExecution?.id
    val assignment = claimOrSettle(job, translationId) ?: return
    val outcome = callLlm(assignment)
    saveIfStillCurrent(job, assignment, outcome, chunkExecutionId)
  }

  private fun claimOrSettle(
    job: BatchJobDto,
    translationId: Long,
  ): Assignment? =
    executeInNewTransaction(transactionManager) {
      val translation =
        entityManager.find(Translation::class.java, translationId) ?: return@executeInNewTransaction null
      val row = reviewRepository.findByTranslationId(translationId) ?: return@executeInNewTransaction null
      if (!AiReviewEligibility.isQueued(row)) return@executeInNewTransaction null
      val textHash = AiReviewEligibility.currentTextHash(translation)
      if (row.textHash != textHash) {
        // A missed text-change event leaves the hash stale and the row queued forever.
        refreshForCurrentText(row, textHash)
        entityManager.flush()
      }

      val project = translation.key.project
      // Losing the feature stops processing but keeps the queued rows for a later regain.
      if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) {
        return@executeInNewTransaction null
      }
      val aiManaged = AiReviewEligibility.isAiManagedReview(translation, row)
      val decision =
        decideOrSettle(translation, row, aiManaged, project.baseLanguage?.id)
          ?: return@executeInNewTransaction null
      when (qaGateFor(translation, project, decision)) {
        QaGateResult.PASS -> Unit
        QaGateResult.WAIT_FOR_FRESH_QA -> return@executeInNewTransaction null
        QaGateResult.OPEN_ISSUES -> {
          markSkippedForQaIssues(row)
          return@executeInNewTransaction null
        }
      }
      claim(
        job,
        translation,
        project,
        decision,
        textHash,
        aiManaged,
        requeuedHumanDecision = AiReviewEligibility.isRequeuedHumanDecision(row),
      )
    }

  private fun decideOrSettle(
    translation: Translation,
    row: TranslationAiReview,
    aiManaged: Boolean,
    baseLanguageId: Long?,
  ): RuleDecision.Review? {
    if (AiReviewEligibility.isTrashed(translation)) return null
    val decision = decisionIfEligible(translation, row, aiManaged, baseLanguageId)
    if (decision is RuleDecision.Review) return decision
    if (aiManaged) {
      leaveQueueAiManaged(row)
      return null
    }
    deleteUnlessResolved(row)
    return null
  }

  private fun decisionIfEligible(
    translation: Translation,
    row: TranslationAiReview,
    aiManaged: Boolean,
    baseLanguageId: Long?,
  ): RuleDecision? {
    if (!AiReviewEligibility.isEligible(translation, baseLanguageId, row)) return null
    return evaluator.evaluate(translation, activeOnly = aiManaged)
  }

  /**
   * The row must stay, or the Reviewed state would look like a human review. A PENDING row has no
   * result to fall back to, so it becomes FAILED, which keeps it AI-managed but out of the queue.
   */
  private fun leaveQueueAiManaged(row: TranslationAiReview) {
    row.reReviewRequested = false
    if (row.status == AiReviewStatus.PENDING) row.status = AiReviewStatus.FAILED
  }

  private fun qaGateFor(
    translation: Translation,
    project: Project,
    decision: RuleDecision.Review,
  ): QaGateResult {
    if (!qaGate.isQaOnForLanguage(project, translation.language.id)) return QaGateResult.PASS
    if (translation.qaChecksStale) return QaGateResult.WAIT_FOR_FRESH_QA
    if (!decision.rule.skipOpenQaIssues) return QaGateResult.PASS
    if (!qaGate.hasOpenQaIssues(translation.id)) return QaGateResult.PASS
    return QaGateResult.OPEN_ISSUES
  }

  private fun markSkippedForQaIssues(row: TranslationAiReview) {
    row.status = AiReviewStatus.SKIPPED_QA_ISSUES
    row.reReviewRequested = false
  }

  private fun claim(
    job: BatchJobDto,
    translation: Translation,
    project: Project,
    decision: RuleDecision.Review,
    textHash: String,
    aiManaged: Boolean,
    requeuedHumanDecision: Boolean,
  ): Assignment? {
    val prompt = decision.rule.promptId?.let { promptRepository.findById(it).orElse(null) }
    val assigned = reviewRepository.claimForJob(jobId = job.id, translationId = translation.id, textHash = textHash)
    if (assigned == 0) return null

    return Assignment(
      translation = translation,
      projectId = project.id,
      organizationId = project.organizationOwner.id,
      baseLanguageId = project.baseLanguage?.id,
      textHash = textHash,
      ruleId = decision.rule.id,
      ruleMode = decision.rule.mode,
      promptId = prompt?.id,
      promptName = prompt?.name ?: DefaultReviewPromptHelper.NAME,
      promptRevision = prompt?.revision,
      aiManaged = aiManaged,
      requeuedHumanDecision = requeuedHumanDecision,
    )
  }

  private fun callLlm(assignment: Assignment): LlmOutcome {
    try {
      val result =
        aiReviewPromptService.runReview(
          organizationId = assignment.organizationId,
          projectId = assignment.projectId,
          translation = assignment.translation,
          promptId = assignment.promptId,
          priority = LlmProviderPriority.LOW,
        )
      return LlmOutcome.Success(result.issues, result.score, result.providerName, result.model)
    } catch (e: LlmRateLimitedException) {
      // Must stay before its parent FailedDependencyException: AiReviewChunkProcessor retries after retryAt.
      throw e
    } catch (e: OutOfCreditsException) {
      return LlmOutcome.Failure(AiReviewFailureReason.OUT_OF_CREDITS)
    } catch (e: LlmProviderNotFoundException) {
      return LlmOutcome.Failure(AiReviewFailureReason.NO_PROVIDER)
    } catch (e: ReviewParseException) {
      return LlmOutcome.Failure(AiReviewFailureReason.PARSE_ERROR)
    } catch (e: BadRequestException) {
      if (e.tolgeeMessage == Message.LLM_TEMPLATE_PARSING_ERROR) {
        return LlmOutcome.Failure(AiReviewFailureReason.TEMPLATE_ERROR)
      }
      throw e
    } catch (e: FailedDependencyException) {
      return LlmOutcome.Failure(AiReviewFailureReason.PROVIDER_ERROR)
    }
  }

  private fun saveIfStillCurrent(
    job: BatchJobDto,
    assignment: Assignment,
    outcome: LlmOutcome,
    chunkExecutionId: Long?,
  ) {
    val update =
      executeInNewTransaction(transactionManager) {
        val translation =
          entityManager.find(Translation::class.java, assignment.translation.id, LockModeType.PESSIMISTIC_WRITE)
            ?: return@executeInNewTransaction null
        val row = reviewRepository.findByTranslationId(translation.id) ?: return@executeInNewTransaction null
        entityManager.lock(row, LockModeType.PESSIMISTIC_WRITE)

        if (row.batchJobId != job.id || row.textHash != assignment.textHash) return@executeInNewTransaction null
        if (reviewedByHumanSinceClaim(translation, assignment)) {
          return@executeInNewTransaction releaseToHumanReview(row)
        }
        if (unReviewedByHumanSinceClaim(translation, row, assignment)) {
          return@executeInNewTransaction releaseToUnReview(row)
        }
        val decision =
          currentDecisionOrNull(translation, row, assignment) ?: return@executeInNewTransaction releaseClaim(row)
        when (qaGateFor(translation, translation.key.project, decision)) {
          QaGateResult.PASS -> Unit
          QaGateResult.WAIT_FOR_FRESH_QA -> return@executeInNewTransaction releaseClaim(row)
          QaGateResult.OPEN_ISSUES -> {
            markSkippedForQaIssues(row)
            return@executeInNewTransaction releaseClaim(row)
          }
        }

        markActivityAsAiReviewer(job, assignment, chunkExecutionId)
        recordOutcome(translation, row, outcome, decision, assignment)
        AiReviewUpdate.of(row, dryRun = decision.rule.mode == AiReviewRuleMode.DRY_RUN)
      } ?: return
    websocketPublisher.publishAfterCommit(assignment.projectId, listOf(update))
  }

  private fun currentDecisionOrNull(
    translation: Translation,
    row: TranslationAiReview,
    assignment: Assignment,
  ): RuleDecision.Review? {
    if (AiReviewEligibility.currentTextHash(translation) != assignment.textHash) return null
    if (!AiReviewEligibility.isEligible(translation, assignment.baseLanguageId, row)) return null
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, translation.key.project)) return null
    val decision = evaluator.evaluate(translation, activeOnly = assignment.aiManaged)
    if (decision !is RuleDecision.Review || winnerChanged(decision, assignment)) return null
    return decision
  }

  private fun releaseClaim(row: TranslationAiReview): AiReviewUpdate? {
    row.batchJobId = null
    return null
  }

  /**
   * Only a human can set Reviewed while the AI holds the claim. A claim that started as a requeued
   * human decision is already Reviewed, so the state says nothing there.
   */
  private fun reviewedByHumanSinceClaim(
    translation: Translation,
    assignment: Assignment,
  ): Boolean =
    !assignment.aiManaged &&
      !assignment.requeuedHumanDecision &&
      translation.state == TranslationState.REVIEWED

  private fun unReviewedByHumanSinceClaim(
    translation: Translation,
    row: TranslationAiReview,
    assignment: Assignment,
  ): Boolean {
    if (AiReviewEligibility.isHumanDecision(row) && !assignment.requeuedHumanDecision) return true
    return assignment.aiManaged && translation.state != TranslationState.REVIEWED
  }

  private fun releaseToHumanReview(row: TranslationAiReview): AiReviewUpdate? {
    if (!AiReviewEligibility.keepsAiManaged(row)) return releaseClaim(row)
    reviewRepository.delete(row)
    return AiReviewUpdate.removed(row.translation)
  }

  private fun releaseToUnReview(row: TranslationAiReview): AiReviewUpdate? {
    if (!AiReviewEligibility.isMarker(row)) {
      row.reReviewRequested = false
      return releaseClaim(row)
    }
    reviewRepository.delete(row)
    return AiReviewUpdate.removed(row.translation)
  }

  private fun markActivityAsAiReviewer(
    job: BatchJobDto,
    assignment: Assignment,
    chunkExecutionId: Long?,
  ) {
    activityHolder.activityRevision.also {
      it.systemActor = SystemActor.AI_REVIEWER
      it.authorId = job.authorId
      it.projectId = assignment.projectId
      it.organizationId = assignment.organizationId
      it.batchJobChunkExecution =
        chunkExecutionId?.let { id ->
          entityManager.getReference(BatchJobChunkExecution::class.java, id)
        }
    }
  }

  private fun winnerChanged(
    decision: RuleDecision.Review,
    assignment: Assignment,
  ): Boolean {
    if (decision.rule.id != assignment.ruleId) return true
    if (decision.rule.mode != assignment.ruleMode) return true
    if (decision.rule.promptId != assignment.promptId) return true
    val currentRevision = assignment.promptId?.let { promptRepository.findById(it).orElse(null)?.revision }
    return currentRevision != assignment.promptRevision
  }

  private fun recordOutcome(
    translation: Translation,
    row: TranslationAiReview,
    outcome: LlmOutcome,
    decision: RuleDecision.Review,
    assignment: Assignment,
  ) {
    val keepsPreviousResult = outcome is LlmOutcome.Failure && row.score != null
    when (outcome) {
      is LlmOutcome.Success -> applySuccess(translation, row, outcome, decision)
      is LlmOutcome.Failure -> applyFailure(row, outcome)
    }
    if (!keepsPreviousResult) {
      attributeResult(row, decision, assignment)
      row.resolvedBy = null
      row.resolvedAt = null
    }
    row.batchJobId = null
    row.reReviewRequested = false
    row.reviewedAt = currentDateProvider.date
  }

  private fun applySuccess(
    translation: Translation,
    row: TranslationAiReview,
    outcome: LlmOutcome.Success,
    decision: RuleDecision.Review,
  ) {
    val status = AiReviewScoring.outcome(decision.rule.mode, outcome.score, decision.threshold)
    val wasAiManagedReview = AiReviewEligibility.isAiManagedReview(translation, row)
    if (status == AiReviewStatus.APPROVED && !isApprovalOf(row, decision.rule.id)) {
      ruleRepository.addToAutoReviewedCount(decision.rule.id, 1)
    }
    row.status = status
    row.score = outcome.score
    row.issues = outcome.issues.toMutableList()
    row.providerName = outcome.providerName
    row.model = outcome.model
    row.failureReason = null
    row.lastFailureReason = null
    row.lastFailedAt = null

    if (status == AiReviewStatus.APPROVED) {
      translation.state = TranslationState.REVIEWED
      return
    }
    if (status == AiReviewStatus.BELOW_THRESHOLD && wasAiManagedReview &&
      !AiReviewEligibility.isProtectedReviewed(translation)
    ) {
      translation.state = TranslationState.TRANSLATED
    }
  }

  private fun isApprovalOf(
    row: TranslationAiReview,
    ruleId: Long,
  ): Boolean = row.status == AiReviewStatus.APPROVED && row.rule?.id == ruleId

  private fun attributeResult(
    row: TranslationAiReview,
    decision: RuleDecision.Review,
    assignment: Assignment,
  ) {
    row.rule = entityManager.getReference(AiReviewRule::class.java, decision.rule.id)
    row.ruleMode = decision.rule.mode
    row.threshold = decision.threshold
    row.promptId = assignment.promptId
    row.promptName = assignment.promptName
    row.promptRevision = assignment.promptRevision
  }

  private fun applyFailure(
    row: TranslationAiReview,
    outcome: LlmOutcome.Failure,
  ) {
    if (row.score != null) {
      row.lastFailureReason = outcome.reason
      row.lastFailedAt = currentDateProvider.date
      return
    }
    row.status = AiReviewStatus.FAILED
    row.failureReason = outcome.reason
  }

  private fun refreshForCurrentText(
    row: TranslationAiReview,
    textHash: String,
  ) {
    row.textHash = textHash
    row.status = AiReviewStatus.PENDING
    row.score = null
    row.issues = mutableListOf()
    row.failureReason = null
    row.lastFailureReason = null
    row.lastFailedAt = null
  }

  private fun deleteUnlessResolved(row: TranslationAiReview) {
    if (AiReviewEligibility.isHumanDecision(row)) {
      // A requeued human decision with no winner any more must still leave the queue.
      row.reReviewRequested = false
      return
    }
    reviewRepository.delete(row)
  }
}
