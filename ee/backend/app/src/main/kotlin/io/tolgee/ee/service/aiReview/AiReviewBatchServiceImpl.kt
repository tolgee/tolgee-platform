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
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.exceptions.LlmProviderNotFoundException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.Project
import io.tolgee.model.aiReview.AiReviewIssue
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.SystemActor
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFailureReason
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.translation.Translation
import io.tolgee.repository.PromptRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.aiReview.AiReviewBatchService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.util.executeInNewTransaction
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.apache.commons.codec.digest.DigestUtils.sha256Hex
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

@Primary
@Service
class AiReviewBatchServiceImpl(
  private val evaluator: AiReviewRuleEvaluator,
  private val aiReviewPromptService: AiReviewPromptService,
  private val reviewRepository: TranslationAiReviewRepository,
  private val promptRepository: PromptRepository,
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectQaConfigService: ProjectQaConfigService,
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
    val promptRevision: Int?,
    val aiManaged: Boolean,
  )

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
    val assignment = prepareAndAssignToJob(job, translationId) ?: return
    val outcome = callLlm(assignment)
    save(job, assignment, outcome)
  }

  private fun prepareAndAssignToJob(
    job: BatchJobDto,
    translationId: Long,
  ): Assignment? =
    executeInNewTransaction(transactionManager) {
      val translation =
        entityManager.find(Translation::class.java, translationId) ?: return@executeInNewTransaction null
      val row = reviewRepository.findByTranslationId(translationId) ?: return@executeInNewTransaction null
      if (!isQueuedForReview(row)) return@executeInNewTransaction null
      val textHash = sha256Hex(translation.text ?: "")
      if (row.textHash != textHash) return@executeInNewTransaction null

      val project = translation.key.project
      // Losing the feature stops processing but keeps the queued rows for a later regain.
      if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) {
        return@executeInNewTransaction null
      }
      val baseLanguageId = project.baseLanguage?.id
      if (!AiReviewEligibility.isEligible(translation, baseLanguageId, row)) {
        deleteUnlessResolved(row)
        return@executeInNewTransaction null
      }

      val aiManaged = AiReviewEligibility.isAiManagedReview(translation, row)
      val decision = evaluator.evaluate(translation, activeOnly = aiManaged)
      if (decision !is RuleDecision.Review) {
        if (aiManaged) {
          row.reReviewRequested = false
        } else {
          deleteUnlessResolved(row)
        }
        return@executeInNewTransaction null
      }

      if (isQaOnForLanguage(project, translation.language.id)) {
        if (translation.qaChecksStale) return@executeInNewTransaction null
        if (decision.rule.skipOpenQaIssues && hasOpenQaIssues(translationId)) {
          row.status = AiReviewStatus.SKIPPED_QA_ISSUES
          row.reReviewRequested = false
          return@executeInNewTransaction null
        }
      }

      val promptId = decision.rule.promptId
      val prompt = promptId?.let { promptRepository.findById(it).orElse(null) }
      val promptRevision = prompt?.revision
      val assigned =
        reviewRepository.assignToJob(
          jobId = job.id,
          translationId = translationId,
          textHash = textHash,
          ruleId = decision.rule.id,
          ruleMode = decision.rule.mode.name,
          promptId = prompt?.id,
          promptName = prompt?.name ?: DefaultReviewPromptHelper.NAME,
          promptRevision = promptRevision,
          threshold = decision.threshold,
        )
      if (assigned == 0) return@executeInNewTransaction null

      Assignment(
        translation = translation,
        projectId = project.id,
        organizationId = project.organizationOwner.id,
        baseLanguageId = baseLanguageId,
        textHash = textHash,
        ruleId = decision.rule.id,
        ruleMode = decision.rule.mode,
        promptId = prompt?.id,
        promptRevision = promptRevision,
        aiManaged = aiManaged,
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

  private fun save(
    job: BatchJobDto,
    assignment: Assignment,
    outcome: LlmOutcome,
  ) {
    val update =
      executeInNewTransaction(transactionManager) {
        val translation =
          entityManager.find(Translation::class.java, assignment.translation.id, LockModeType.PESSIMISTIC_WRITE)
            ?: return@executeInNewTransaction null
        val row = reviewRepository.findByTranslationId(translation.id) ?: return@executeInNewTransaction null
        entityManager.lock(row, LockModeType.PESSIMISTIC_WRITE)

        if (row.batchJobId != job.id || row.textHash != assignment.textHash) return@executeInNewTransaction null
        if (sha256Hex(translation.text ?: "") != assignment.textHash ||
          !AiReviewEligibility.isEligible(translation, assignment.baseLanguageId, row)
        ) {
          row.batchJobId = null
          return@executeInNewTransaction null
        }

        val decision = evaluator.evaluate(translation, activeOnly = assignment.aiManaged)
        if (decision !is RuleDecision.Review || winnerChanged(decision, assignment)) {
          row.batchJobId = null
          return@executeInNewTransaction null
        }

        markActivityAsAiReviewer(job, assignment)
        apply(translation, row, outcome, decision, assignment)
        toUpdate(translation, row, decision)
      } ?: return
    websocketPublisher.publishAfterCommit(assignment.projectId, listOf(update))
  }

  /**
   * The chunk's ActivityHolder is transaction-scoped, so the systemActor that
   * ChunkProcessingUtil.handleActivity sets never reaches this inner transaction. The state change
   * is logged here, so the revision of this transaction gets the same actor data.
   */
  private fun markActivityAsAiReviewer(
    job: BatchJobDto,
    assignment: Assignment,
  ) {
    activityHolder.activityRevision.also {
      it.systemActor = SystemActor.AI_REVIEWER
      it.authorId = job.authorId
      it.projectId = assignment.projectId
      it.organizationId = assignment.organizationId
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

  private fun apply(
    translation: Translation,
    row: TranslationAiReview,
    outcome: LlmOutcome,
    decision: RuleDecision.Review,
    assignment: Assignment,
  ) {
    when (outcome) {
      is LlmOutcome.Success -> applySuccess(translation, row, outcome, decision)
      is LlmOutcome.Failure -> applyFailure(row, outcome)
    }
    row.threshold = decision.threshold
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
    if (status == AiReviewStatus.BELOW_THRESHOLD && wasAiManagedReview && !isProtected(translation)) {
      translation.state = TranslationState.TRANSLATED
    }
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

  private fun toUpdate(
    translation: Translation,
    row: TranslationAiReview,
    decision: RuleDecision.Review,
  ) = AiReviewUpdate(
    translationId = translation.id,
    keyId = translation.key.id,
    languageTag = translation.language.tag,
    status = row.status,
    score = row.score,
    dryRun = decision.rule.mode == AiReviewRuleMode.DRY_RUN,
    lastFailed = row.lastFailureReason != null,
  )

  private fun isProtected(translation: Translation): Boolean =
    translation.key.project.translationProtection == TranslationProtection.PROTECT_REVIEWED &&
      translation.state == TranslationState.REVIEWED

  private fun isQueuedForReview(row: TranslationAiReview): Boolean =
    row.status == AiReviewStatus.PENDING || row.reReviewRequested

  private fun deleteUnlessResolved(row: TranslationAiReview) {
    if (row.status == AiReviewStatus.CONFIRMED || row.status == AiReviewStatus.REJECTED) return
    reviewRepository.delete(row)
  }

  /**
   * Matches the readiness check of AiReviewQueueService: for a language with QA off,
   * qaChecksStale never clears, so gating on it would leave the row queued forever.
   */
  private fun isQaOnForLanguage(
    project: Project,
    languageId: Long,
  ): Boolean {
    if (!project.useQaChecks) return false
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return false
    return languageId in projectQaConfigService.getEnabledLanguageIds(project.id)
  }

  private fun hasOpenQaIssues(translationId: Long): Boolean =
    qaIssueRepository.findAllByTranslationId(translationId).any { it.state == QaIssueState.OPEN }
}
