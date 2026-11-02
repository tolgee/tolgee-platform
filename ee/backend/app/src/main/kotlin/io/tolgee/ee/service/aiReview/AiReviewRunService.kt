package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.constants.Feature
import io.tolgee.ee.data.aiReview.AiReviewRunPreviewModel
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.data.aiReview.AiReviewSkipReason
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.SecurityService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRunService(
  private val evaluator: AiReviewRuleEvaluator,
  private val statsService: AiReviewRuleStatsService,
  private val reviewRepository: TranslationAiReviewRepository,
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val queueService: AiReviewQueueService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectQaConfigService: ProjectQaConfigService,
  private val languageService: LanguageService,
  private val securityService: SecurityService,
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
    val classified = classify(projectId, request)
    return AiReviewRunPreviewModel(
      toReview = classified.toReview.size.toLong(),
      dryRunOnly = classified.dryRunOnly,
      skipped = classified.skipped,
    )
  }

  /**
   * The start never fails because another job runs; the work waits in the queue. Rows whose QA is
   * still stale stay queued: the processor leaves them and the QA trigger picks them up later.
   */
  @Transactional
  fun start(
    projectId: Long,
    request: AiReviewRunRequest,
    author: UserAccount,
  ): BatchJob {
    val classified = classify(projectId, request)
    val projectRef = entityManager.getReference(Project::class.java, projectId)
    val job =
      batchJobService.startJob(
        request = AiReviewRequest(translationIds = classified.toReview.map { it.translationId }),
        project = projectRef,
        author = author,
        type = BatchJobType.AI_REVIEW,
        isHidden = false,
      )
    classified.toReview.forEach { item ->
      if (item.hasRow) {
        val row = reviewRepository.findByTranslationId(item.translationId) ?: return@forEach
        row.reReviewRequested = true
        row.manualRunBatchJobId = job.id
        return@forEach
      }
      queueService.queue(projectId, listOf(item.translationId), manualRunBatchJobId = job.id)
    }
    return job
  }

  /**
   * Cancel undoes only what this run queued. A row replaced by a text change in the meantime has no
   * manualRunBatchJobId any more and is not touched.
   */
  @Transactional
  fun onRunCancelled(jobId: Long) {
    reviewRepository.deletePendingOfManualRun(jobId)
    reviewRepository.clearReReviewOfManualRun(jobId)
  }

  private fun classify(
    projectId: Long,
    request: AiReviewRunRequest,
  ): Classified {
    val project = projectService.getDto(projectId)
    projectFeatureGuard.checkEnabled(Feature.AI_REVIEW, project)
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    val qaOn =
      project.useQaChecks && projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)
    val qaLanguageIds = if (qaOn) projectQaConfigService.getEnabledLanguageIds(projectId) else emptySet()
    val stateEditAllowed = mutableMapOf<Long, Boolean>()

    val toReview = mutableListOf<ToReview>()
    var dryRunOnly = 0L
    val skipped = mutableMapOf<AiReviewSkipReason, Long>()

    candidateTranslationIds(projectId, request).distinct().forEach { translationId ->
      val translation =
        entityManager.find(Translation::class.java, translationId) ?: return@forEach
      if (translation.key.project.id != projectId) return@forEach
      if (translation.language.id == baseLanguageId) return@forEach

      val row = reviewRepository.findByTranslationId(translationId)
      val reason = skipReason(translation, row, request, qaLanguageIds, stateEditAllowed, projectId)
      if (reason != null) {
        skipped.merge(reason, 1L, Long::plus)
        return@forEach
      }
      val decision = evaluator.evaluate(translation, activeOnly = isAiManaged(translation, row))
      if ((decision as RuleDecision.Review).rule.mode == AiReviewRuleMode.DRY_RUN) dryRunOnly++
      toReview += ToReview(translationId, hasRow = row != null)
    }
    return Classified(toReview, dryRunOnly, skipped)
  }

  private fun skipReason(
    translation: Translation,
    row: io.tolgee.model.aiReview.TranslationAiReview?,
    request: AiReviewRunRequest,
    qaLanguageIds: Set<Long>,
    stateEditAllowed: MutableMap<Long, Boolean>,
    projectId: Long,
  ): AiReviewSkipReason? {
    if (translation.text.isNullOrBlank() || translation.state == TranslationState.DISABLED) {
      return AiReviewSkipReason.EMPTY
    }
    if (translation.outdated) return AiReviewSkipReason.OUTDATED
    if (AiReviewEligibility.isHumanReviewed(translation, row)) return AiReviewSkipReason.HUMAN_REVIEWED

    val decision = evaluator.evaluate(translation, activeOnly = isAiManaged(translation, row))
    if (decision is RuleDecision.Never) return AiReviewSkipReason.NEVER
    if (decision !is RuleDecision.Review) return AiReviewSkipReason.NO_RULE

    if (row != null) {
      if (row.status == AiReviewStatus.CONFIRMED) return AiReviewSkipReason.CONFIRMED_BY_HUMAN
      if (row.status == AiReviewStatus.REJECTED) return AiReviewSkipReason.REJECTED_BY_HUMAN
      if (row.status == AiReviewStatus.PENDING || row.reReviewRequested) {
        return AiReviewSkipReason.ALREADY_QUEUED
      }
      val hasResult = row.status in RESULT_STATUSES
      val reRun = request.reReview || (request.failedOnly && row.lastFailureReason != null)
      if (hasResult && !reRun) return AiReviewSkipReason.ALREADY_HAS_RESULT
    }

    if (decision.rule.skipOpenQaIssues &&
      translation.language.id in qaLanguageIds &&
      hasOpenQaIssues(translation.id)
    ) {
      return AiReviewSkipReason.OPEN_QA_ISSUES
    }

    if (decision.rule.mode == AiReviewRuleMode.ACTIVE) {
      val allowed =
        stateEditAllowed.getOrPut(translation.language.id) {
          runCatching {
            securityService.checkLanguageStateChangePermission(projectId, listOf(translation.language.id))
          }.isSuccess
        }
      if (!allowed) return AiReviewSkipReason.NO_PERMISSION
    }
    return null
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
    return keyIds.chunked(IN_CLAUSE_CHUNK).flatMap { chunk ->
      entityManager
        .createQuery(
          "select t.id from Translation t where t.key.id in :keyIds and t.language.id in :languageIds",
          java.lang.Long::class.java,
        ).setParameter("keyIds", chunk)
        .setParameter("languageIds", request.languageIds)
        .resultList
        .map { it.toLong() }
    }
  }

  private fun isAiManaged(
    translation: Translation,
    row: io.tolgee.model.aiReview.TranslationAiReview?,
  ): Boolean = AiReviewEligibility.isAiManagedReview(translation, row)

  private fun hasOpenQaIssues(translationId: Long): Boolean =
    qaIssueRepository.findAllByTranslationId(translationId).any { it.state == QaIssueState.OPEN }

  companion object {
    private val RESULT_STATUSES =
      setOf(
        AiReviewStatus.APPROVED,
        AiReviewStatus.BELOW_THRESHOLD,
        AiReviewStatus.DRY_RUN_PASS,
        AiReviewStatus.DRY_RUN_FAIL,
      )

    private const val IN_CLAUSE_CHUNK = 10_000
  }
}
