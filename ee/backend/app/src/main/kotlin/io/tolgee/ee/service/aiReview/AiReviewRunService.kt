package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.data.aiReview.AiReviewRunPreviewModel
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.data.aiReview.AiReviewSkipReason
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.aiReview.AiReviewEligibleText
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.security.UserAccountService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRunService(
  private val evaluator: AiReviewRuleEvaluator,
  private val statsService: AiReviewRuleStatsService,
  private val reviewRepository: TranslationAiReviewRepository,
  private val queueService: AiReviewQueueService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val qaGate: AiReviewQaGate,
  private val translationRepository: TranslationRepository,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
  private val userAccountService: UserAccountService,
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val websocketPublisher: AiReviewWebsocketPublisher,
) {
  private data class Classified(
    val toReview: List<ToReview>,
    val scoreOnlyRunCount: Long,
    val skipped: Map<AiReviewSkipReason, Long>,
    val overriddenToReReview: Long,
  )

  private data class ToReview(
    val translationId: Long,
    val hasRow: Boolean,
  )

  @Transactional(readOnly = true)
  fun preview(
    projectId: Long,
    request: AiReviewRunRequest,
  ): AiReviewRunPreviewModel {
    val classified = classifyAndClearContext(projectId, request)
    return AiReviewRunPreviewModel(
      toReview = classified.toReview.size.toLong(),
      scoreOnlyRunCount = classified.scoreOnlyRunCount,
      skipped = classified.skipped,
      overriddenToReReview = classified.overriddenToReReview,
    )
  }

  @Transactional
  fun start(
    projectId: Long,
    request: AiReviewRunRequest,
    authorId: Long,
  ): BatchJob {
    val classified = classifyAndClearContext(projectId, request)
    if (classified.toReview.isEmpty()) throw BadRequestException(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    val author = userAccountService.get(authorId)
    val projectRef = entityManager.getReference(Project::class.java, projectId)
    val job =
      batchJobService.startJob(
        request = AiReviewRequest(translationIds = classified.toReview.map { it.translationId }),
        project = projectRef,
        author = author,
        type = BatchJobType.AI_REVIEW,
        isHidden = false,
      )
    val (withRow, withoutRow) = classified.toReview.partition { it.hasRow }
    val reReviewUpdates = requestReReview(withRow.map { it.translationId }, job.id)
    val queuedUpdates =
      queueService.queueAndClearContext(withoutRow.map { it.translationId }, manualRunBatchJobId = job.id)
    websocketPublisher.publishAfterCommit(projectId, reReviewUpdates + queuedUpdates)
    return entityManager.find(BatchJob::class.java, job.id)
  }

  private fun requestReReview(
    translationIds: List<Long>,
    jobId: Long,
  ): List<AiReviewUpdate> {
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(CHUNK).forEach { chunk ->
      reviewRepository.findAllByTranslationIdIn(chunk).forEach { row ->
        row.reReviewRequested = true
        row.manualRunBatchJobId = jobId
        updates += AiReviewUpdate.of(row, scoreOnlyRun = AiReviewUpdate.scoreOnlyRun(row))
      }
    }
    return updates
  }

  /**
   * Cancel undoes only what this run queued: a row replaced in the meantime has no
   * manualRunBatchJobId any more.
   */
  @Transactional
  fun onRunCancelled(jobId: Long) {
    reviewRepository.deletePendingOfManualRun(jobId)
    reviewRepository.clearReReviewOfManualRun(jobId)
  }

  private fun classifyAndClearContext(
    projectId: Long,
    request: AiReviewRunRequest,
  ): Classified {
    val project = projectService.getDto(projectId)
    projectFeatureGuard.checkEnabled(Feature.AI_REVIEW, project)
    val classifier =
      CandidateClassifier(
        projectId = projectId,
        baseLanguageId = languageService.getProjectBaseLanguage(projectId).id,
        request = request,
        projectRules = evaluator.forProject(projectId),
        qaLanguageIds = qaGate.qaLanguageIds(project),
      )

    val toReview = mutableListOf<ToReview>()
    var scoreOnlyRunCount = 0L
    val skipped = mutableMapOf<AiReviewSkipReason, Long>()
    var overriddenToReReview = 0L

    candidateTranslationIds(projectId, request).distinct().chunked(CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllByTranslationIdIn(chunk).associateBy { it.translation.id }
      val openQaIssueIds = openQaIssueIds(chunk, classifier.qaLanguageIds)
      translationRepository.findAllForAiReviewRuleEvaluation(chunk).forEach { translation ->
        when (val result = classifier.decide(translation, rows[translation.id], openQaIssueIds)) {
          is CandidateResult.Skipped -> skipped.merge(result.reason, 1L, Long::plus)
          is CandidateResult.Review -> {
            if (result.scoreOnlyRun) scoreOnlyRunCount++
            if (rows[translation.id]?.status == AiReviewStatus.OVERRIDDEN) overriddenToReReview++
            toReview += ToReview(translation.id, hasRow = result.hasRow)
          }
          CandidateResult.NotACandidate -> Unit
        }
      }
      entityManager.clear()
    }
    return Classified(toReview, scoreOnlyRunCount, skipped, overriddenToReReview)
  }

  private fun openQaIssueIds(
    chunk: List<Long>,
    qaLanguageIds: Set<Long>,
  ): Set<Long> {
    if (qaLanguageIds.isEmpty()) return emptySet()
    return qaGate.translationIdsWithOpenIssues(chunk)
  }

  private sealed interface CandidateResult {
    data class Skipped(
      val reason: AiReviewSkipReason,
    ) : CandidateResult

    data class Review(
      val scoreOnlyRun: Boolean,
      val hasRow: Boolean,
    ) : CandidateResult

    object NotACandidate : CandidateResult
  }

  private inner class CandidateClassifier(
    val projectId: Long,
    val baseLanguageId: Long,
    val request: AiReviewRunRequest,
    val projectRules: AiReviewRuleEvaluator.ProjectRules,
    val qaLanguageIds: Set<Long>,
  ) {
    private val stateEditAllowed = mutableMapOf<Long, Boolean>()
    private val branchModifyAllowed = mutableMapOf<Long?, Boolean>()

    fun decide(
      translation: Translation,
      row: TranslationAiReview?,
      openQaIssueIds: Set<Long>,
    ): CandidateResult {
      if (translation.key.project.id != projectId) return CandidateResult.NotACandidate
      if (translation.language.id == baseLanguageId) return CandidateResult.NotACandidate
      if (AiReviewEligibility.isTrashed(translation)) return CandidateResult.NotACandidate

      basicSkipReason(translation, row)?.let { return CandidateResult.Skipped(it) }
      val decision =
        projectRules.evaluate(translation, activeOnly = AiReviewEligibility.isAiManagedReview(translation, row))
      if (decision is RuleDecision.Never) return CandidateResult.Skipped(AiReviewSkipReason.NEVER)
      if (decision !is RuleDecision.Review) return CandidateResult.Skipped(AiReviewSkipReason.NO_RULE)
      val reason =
        rowSkipReason(row, decision)
          ?: qaSkipReason(decision, translation, openQaIssueIds)
          ?: permissionSkipReason(decision, translation)
      if (reason != null) return CandidateResult.Skipped(reason)

      return CandidateResult.Review(
        scoreOnlyRun = decision.rule.mode == AiReviewRuleMode.SCORE_ONLY,
        hasRow = row != null,
      )
    }

    private fun basicSkipReason(
      translation: Translation,
      row: TranslationAiReview?,
    ): AiReviewSkipReason? {
      if (!AiReviewEligibleText.hasReviewableText(translation) || translation.state == TranslationState.DISABLED) {
        return AiReviewSkipReason.EMPTY
      }
      if (translation.outdated) return AiReviewSkipReason.OUTDATED
      if (AiReviewEligibility.isHumanReviewed(translation, row) && !reRunsHumanDecision(row)) {
        return AiReviewSkipReason.HUMAN_REVIEWED
      }
      return null
    }

    private fun reRunsHumanDecision(row: TranslationAiReview?): Boolean =
      request.reReview && row != null && AiReviewEligibility.isHumanDecision(row)

    private fun rowSkipReason(
      row: TranslationAiReview?,
      decision: RuleDecision.Review,
    ): AiReviewSkipReason? {
      if (row == null) return null
      if (!request.reReview && row.status == AiReviewStatus.OVERRIDDEN) return AiReviewSkipReason.OVERRIDDEN
      if (AiReviewEligibility.isQueued(row)) return AiReviewSkipReason.ALREADY_QUEUED
      val scoreOnlyRerun =
        decision.rule.mode == AiReviewRuleMode.ACTIVE && row.status in SCORE_ONLY_STATUSES
      val hasResult = row.status in RESULT_STATUSES
      val reRun = request.reReview || scoreOnlyRerun || (request.failedOnly && row.lastFailureReason != null)
      if (hasResult && !reRun) return AiReviewSkipReason.ALREADY_HAS_RESULT
      return null
    }

    private fun qaSkipReason(
      decision: RuleDecision.Review,
      translation: Translation,
      openQaIssueIds: Set<Long>,
    ): AiReviewSkipReason? {
      if (!decision.rule.skipOpenQaIssues) return null
      if (translation.language.id !in qaLanguageIds) return null
      if (translation.id !in openQaIssueIds) return null
      return AiReviewSkipReason.OPEN_QA_ISSUES
    }

    private fun permissionSkipReason(
      decision: RuleDecision.Review,
      translation: Translation,
    ): AiReviewSkipReason? {
      if (decision.rule.mode != AiReviewRuleMode.ACTIVE) return null
      val stateEdit =
        stateEditAllowed.getOrPut(translation.language.id) {
          runCatching {
            securityService.checkLanguageStateChangePermission(projectId, listOf(translation.language.id))
          }.isSuccess
        }
      if (!stateEdit) return AiReviewSkipReason.NO_PERMISSION
      val branchModify =
        branchModifyAllowed.getOrPut(translation.key.branch?.id) {
          runCatching { securityService.checkBranchModify(translation) }.isSuccess
        }
      if (!branchModify) return AiReviewSkipReason.NO_PERMISSION
      return null
    }
  }

  private fun candidateTranslationIds(
    projectId: Long,
    request: AiReviewRunRequest,
  ): List<Long> {
    request.translationIds?.let { return it }
    request.ruleId?.let { ruleId ->
      if (request.failedOnly) return reviewRepository.findFailedTranslationIdsByRule(ruleId)
      val ids = mutableListOf<Long>()
      statsService.forEachBacklogItem(projectId, ruleId) { ids += it }
      return ids
    }
    val keyIds = request.keyIds ?: return emptyList()
    if (keyIds.isEmpty() || request.languageIds.isEmpty()) return emptyList()
    return keyIds.chunked(IN_CLAUSE_CHUNK).flatMap {
      translationRepository.getIdsByKeyIdsAndLanguageIds(it, request.languageIds)
    }
  }

  companion object {
    private val RESULT_STATUSES =
      setOf(
        AiReviewStatus.APPROVED,
        AiReviewStatus.BELOW_THRESHOLD,
        AiReviewStatus.SCORE_ONLY_PASS,
        AiReviewStatus.SCORE_ONLY_FAIL,
      )

    private val SCORE_ONLY_STATUSES =
      setOf(
        AiReviewStatus.SCORE_ONLY_PASS,
        AiReviewStatus.SCORE_ONLY_FAIL,
      )

    private const val CHUNK = 1_000
  }
}
