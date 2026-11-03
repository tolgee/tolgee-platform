package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewDemotionRequest
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewQueueService(
  private val evaluator: AiReviewRuleEvaluator,
  private val reviewRepository: TranslationAiReviewRepository,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val qaGate: AiReviewQaGate,
  private val translationRepository: TranslationRepository,
  private val languageService: LanguageService,
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val websocketPublisher: AiReviewWebsocketPublisher,
) {
  /**
   * The old row is deleted even when AI_REVIEW is off: a row must never describe text it did
   * not review.
   */
  @Transactional
  fun onTextChanged(
    projectId: Long,
    translationIds: Collection<Long>,
  ) {
    if (translationIds.isEmpty()) return
    if (aiReviewProject(projectId) == null) {
      translationIds.chunked(IN_CLAUSE_CHUNK).forEach { reviewRepository.deleteAllByTranslationIds(it) }
      return
    }
    val projectRules = evaluator.forProject(projectId)
    if (projectRules.rules.isEmpty()) {
      deleteRowsAndPublish(projectId, translationIds)
      return
    }
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id

    val queuedIds = mutableListOf<Long>()
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val oldRows = reviewRepository.findAllByTranslationIdIn(chunk).associateBy { it.translation.id }
      val toQueue = mutableListOf<Translation>()
      var deleted = false
      translationRepository.findAllForAiReviewRuleEvaluation(chunk).forEach { translation ->
        val oldRow = oldRows[translation.id]
        val eligible = AiReviewEligibility.isEligible(translation, baseLanguageId, oldRow)
        val aiManaged = AiReviewEligibility.isAiManagedReview(translation, oldRow)
        oldRow?.let {
          reviewRepository.delete(it)
          deleted = true
        }
        val requeue =
          eligible && projectRules.evaluate(translation, activeOnly = aiManaged) is RuleDecision.Review
        if (oldRow != null || requeue) {
          updates += removedOrPendingUpdate(translation, requeue)
        }
        if (requeue) toQueue += translation
      }
      // The activity event fires after its transaction's last flush, so an unflushed change is lost
      // at commit. The flush also keeps the deletes ahead of the inserts into the unique column.
      if (deleted) entityManager.flush()
      toQueue.forEach { queue(it) }
      queuedIds += toQueue.map { it.id }
    }
    if (queuedIds.isNotEmpty()) entityManager.flush()
    websocketPublisher.publishAfterCommit(projectId, updates)
    startJobsForReady(projectId, queuedIds)
  }

  private fun deleteRowsAndPublish(
    projectId: Long,
    translationIds: Collection<Long>,
  ) {
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllByTranslationIdIn(chunk)
      if (rows.isEmpty()) return@forEach
      val translations =
        translationRepository
          .findAllForAiReviewRuleEvaluation(rows.map { it.translation.id })
          .associateBy { it.id }
      rows.forEach { row ->
        translations[row.translation.id]?.let { updates += removedOrPendingUpdate(it, requeue = false) }
        reviewRepository.delete(row)
      }
    }
    if (updates.isEmpty()) return
    entityManager.flush()
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  private fun removedOrPendingUpdate(
    translation: Translation,
    requeue: Boolean,
  ) = AiReviewUpdate(
    translationId = translation.id,
    keyId = translation.key.id,
    languageTag = translation.language.tag,
    status = if (requeue) AiReviewStatus.PENDING else null,
    score = null,
    dryRun = false,
    lastFailed = false,
  )

  @Transactional
  fun onSourceChanged(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    keyIds.chunked(IN_CLAUSE_CHUNK).forEach {
      reviewRepository.deleteSiblingRows(it, baseLanguageId, STALE_ON_SOURCE_CHANGE)
    }
  }

  @Transactional
  fun onBaseLanguageChanged(projectId: Long) {
    reviewRepository.deleteByProject(projectId, AiReviewEligibility.HUMAN_DECISIONS)
  }

  // Reads come first, writes after: a lookup between inserts would flush them one by one.
  @Transactional
  fun queueAndClearContext(
    translationIds: Collection<Long>,
    manualRunBatchJobId: Long? = null,
  ): Int {
    var queued = 0
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val existing =
        reviewRepository
          .findAllByTranslationIdIn(chunk)
          .map { it.translation.id }
          .toSet()
      translationRepository
        .findAllForAiReviewRuleEvaluation(chunk)
        .filter { it.id !in existing }
        .forEach { translation ->
          queue(translation, manualRunBatchJobId)
          queued++
        }
      entityManager.flush()
      entityManager.clear()
    }
    return queued
  }

  private fun queue(
    translation: Translation,
    manualRunBatchJobId: Long? = null,
  ) {
    reviewRepository.save(
      TranslationAiReview(translation).apply {
        textHash = AiReviewEligibility.currentTextHash(translation)
        status = AiReviewStatus.PENDING
        this.manualRunBatchJobId = manualRunBatchJobId
      },
    )
  }

  @Transactional
  fun startJobsForReady(
    projectId: Long,
    translationIds: Collection<Long>,
    handlingStuckItems: Boolean = false,
  ) {
    if (translationIds.isEmpty()) return
    val project = aiReviewProject(projectId) ?: return

    val ready = findReady(projectId, translationIds, qaGate.qaLanguageIds(project))
    startJob(projectId, ready, handlingStuckItems)
  }

  @Transactional
  fun onQaIssuesIgnored(
    projectId: Long,
    translationIds: Collection<Long>,
  ) {
    if (translationIds.isEmpty()) return
    aiReviewProject(projectId) ?: return

    val requeued = mutableListOf<Long>()
    translationIds.forEach { translationId ->
      val row = reviewRepository.findByTranslationId(translationId) ?: return@forEach
      if (row.status != AiReviewStatus.SKIPPED_QA_ISSUES) return@forEach
      if (qaGate.hasOpenQaIssues(translationId)) return@forEach
      row.status = AiReviewStatus.PENDING
      requeued += translationId
    }
    if (requeued.isEmpty()) return
    entityManager.flush()
    startJobsForReady(projectId, requeued)
  }

  @Transactional
  fun selfHeal(
    projectId: Long,
    finishedJobId: Long? = null,
  ) {
    val project = aiReviewProject(projectId) ?: return
    if (batchJobService.hasActiveJobForProject(projectId, BatchJobType.AI_REVIEW, finishedJobId)) return
    reviewRepository.releaseStaleClaims(projectId)
    val ready = findReadyProjectWide(projectId, qaGate.qaLanguageIds(project))
    startJob(projectId, ready, handlingStuckItems = true)
  }

  @Transactional
  fun startDemotionJob(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    aiReviewProject(projectId) ?: return
    val projectRef = entityManager.getReference(Project::class.java, projectId)
    batchJobService.startJob(
      request = AiReviewDemotionRequest(keyIds = keyIds.toList()),
      project = projectRef,
      author = null,
      type = BatchJobType.AI_REVIEW_DEMOTION,
      isHidden = true,
    )
  }

  @Transactional
  fun startDemotionJobForApprovals(projectId: Long) {
    startDemotionJob(projectId, reviewRepository.findApprovedKeyIds(projectId))
  }

  /**
   * One hidden job per call, no debounce: the batch debounce would return the existing job and
   * drop the new request's targets.
   */
  private fun startJob(
    projectId: Long,
    translationIds: List<Long>,
    handlingStuckItems: Boolean,
  ) {
    if (translationIds.isEmpty()) return
    val projectRef = entityManager.getReference(Project::class.java, projectId)
    batchJobService.startJob(
      request = AiReviewRequest(translationIds = translationIds, handlingStuckItems = handlingStuckItems),
      project = projectRef,
      author = null,
      type = BatchJobType.AI_REVIEW,
      isHidden = true,
    )
  }

  private fun aiReviewProject(projectId: Long): ProjectDto? {
    val project = projectService.getDto(projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) return null
    return project
  }

  private fun findReady(
    projectId: Long,
    translationIds: Collection<Long>,
    qaLanguageIds: Set<Long>,
  ): List<Long> =
    translationIds.chunked(IN_CLAUSE_CHUNK).flatMap { chunk ->
      findReadyChunk(projectId, chunk, qaLanguageIds)
    }

  private fun findReadyChunk(
    projectId: Long,
    chunk: List<Long>,
    qaLanguageIds: Set<Long>,
  ): List<Long> {
    if (qaLanguageIds.isEmpty()) return reviewRepository.findClaimableTranslationIds(projectId, chunk)
    return reviewRepository.findReadyTranslationIds(projectId, chunk, qaLanguageIds)
  }

  private fun findReadyProjectWide(
    projectId: Long,
    qaLanguageIds: Set<Long>,
  ): List<Long> {
    if (qaLanguageIds.isEmpty()) return reviewRepository.findClaimableTranslationIds(projectId)
    return reviewRepository.findReadyTranslationIds(projectId, qaLanguageIds)
  }

  companion object {
    private val STALE_ON_SOURCE_CHANGE =
      setOf(
        AiReviewStatus.PENDING,
        AiReviewStatus.DRY_RUN_PASS,
        AiReviewStatus.DRY_RUN_FAIL,
        AiReviewStatus.BELOW_THRESHOLD,
        AiReviewStatus.FAILED,
        AiReviewStatus.SKIPPED_QA_ISSUES,
      )
  }
}
