package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.constants.Feature
import io.tolgee.ee.data.aiReview.AiReviewRunPreviewModel
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.data.aiReview.AiReviewSkipReason
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
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
) {
  private data class Classified(
    val toReview: List<ToReview>,
    val dryRunOnly: Long,
    val skipped: Map<AiReviewSkipReason, Long>,
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
      dryRunOnly = classified.dryRunOnly,
      skipped = classified.skipped,
    )
  }

  @Transactional
  fun start(
    projectId: Long,
    request: AiReviewRunRequest,
    authorId: Long,
  ): BatchJob {
    val classified = classifyAndClearContext(projectId, request)
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
    withRow.map { it.translationId }.chunked(CHUNK).forEach { chunk ->
      reviewRepository.findAllByTranslationIdIn(chunk).forEach { row ->
        row.reReviewRequested = true
        row.manualRunBatchJobId = job.id
      }
    }
    queueService.queueAndClearContext(withoutRow.map { it.translationId }, manualRunBatchJobId = job.id)
    return entityManager.find(BatchJob::class.java, job.id)
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
    var dryRunOnly = 0L
    val skipped = mutableMapOf<AiReviewSkipReason, Long>()

    candidateTranslationIds(projectId, request).distinct().chunked(CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllByTranslationIdIn(chunk).associateBy { it.translation.id }
      val openQaIssueIds = openQaIssueIds(chunk, classifier.qaLanguageIds)
      translationRepository.findAllForAiReviewRuleEvaluation(chunk).forEach { translation ->
        when (val result = classifier.decide(translation, rows[translation.id], openQaIssueIds)) {
          is CandidateResult.Skipped -> skipped.merge(result.reason, 1L, Long::plus)
          is CandidateResult.Review -> {
            if (result.dryRun) dryRunOnly++
            toReview += ToReview(translation.id, hasRow = result.hasRow)
          }
          CandidateResult.NotACandidate -> Unit
        }
      }
      entityManager.clear()
    }
    return Classified(toReview, dryRunOnly, skipped)
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
      val dryRun: Boolean,
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

    fun decide(
      translation: Translation,
      row: TranslationAiReview?,
      openQaIssueIds: Set<Long>,
    ): CandidateResult {
      if (translation.key.project.id != projectId) return CandidateResult.NotACandidate
      if (translation.language.id == baseLanguageId) return CandidateResult.NotACandidate

      basicSkipReason(translation, row)?.let { return CandidateResult.Skipped(it) }
      val decision =
        projectRules.evaluate(translation, activeOnly = AiReviewEligibility.isAiManagedReview(translation, row))
      if (decision is RuleDecision.Never) return CandidateResult.Skipped(AiReviewSkipReason.NEVER)
      if (decision !is RuleDecision.Review) return CandidateResult.Skipped(AiReviewSkipReason.NO_RULE)
      val reason =
        rowSkipReason(row) ?: qaSkipReason(decision, translation, openQaIssueIds) ?: permissionSkipReason(
          decision,
          translation,
        )
      if (reason != null) return CandidateResult.Skipped(reason)

      return CandidateResult.Review(
        dryRun = decision.rule.mode == AiReviewRuleMode.DRY_RUN,
        hasRow = row != null,
      )
    }

    private fun basicSkipReason(
      translation: Translation,
      row: TranslationAiReview?,
    ): AiReviewSkipReason? {
      if (translation.text.isNullOrBlank() || translation.state == TranslationState.DISABLED) {
        return AiReviewSkipReason.EMPTY
      }
      if (translation.outdated) return AiReviewSkipReason.OUTDATED
      if (AiReviewEligibility.isHumanReviewed(translation, row)) return AiReviewSkipReason.HUMAN_REVIEWED
      return null
    }

    private fun rowSkipReason(row: TranslationAiReview?): AiReviewSkipReason? {
      if (row == null) return null
      if (row.status == AiReviewStatus.CONFIRMED) return AiReviewSkipReason.CONFIRMED_BY_HUMAN
      if (row.status == AiReviewStatus.REJECTED) return AiReviewSkipReason.REJECTED_BY_HUMAN
      if (AiReviewEligibility.isQueued(row)) return AiReviewSkipReason.ALREADY_QUEUED
      val hasResult = row.status in RESULT_STATUSES
      val reRun = request.reReview || (request.failedOnly && row.lastFailureReason != null)
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
      val allowed =
        stateEditAllowed.getOrPut(translation.language.id) {
          runCatching {
            securityService.checkLanguageStateChangePermission(projectId, listOf(translation.language.id))
          }.isSuccess
        }
      if (allowed) return null
      return AiReviewSkipReason.NO_PERMISSION
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
    return translationIdsOfKeys(keyIds, request.languageIds)
  }

  private fun translationIdsOfKeys(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): List<Long> =
    keyIds.chunked(IN_CLAUSE_CHUNK).flatMap { chunk ->
      entityManager
        .createQuery(
          "select t.id from Translation t where t.key.id in :keyIds and t.language.id in :languageIds",
          Long::class.javaObjectType,
        ).setParameter("keyIds", chunk)
        .setParameter("languageIds", languageIds)
        .resultList
    }

  companion object {
    private val RESULT_STATUSES =
      setOf(
        AiReviewStatus.APPROVED,
        AiReviewStatus.BELOW_THRESHOLD,
        AiReviewStatus.DRY_RUN_PASS,
        AiReviewStatus.DRY_RUN_FAIL,
      )

    private const val CHUNK = 1_000
  }
}
