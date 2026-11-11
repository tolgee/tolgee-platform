package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewDemotionRequest
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.Project
import io.tolgee.model.SEQUENCE_NAME
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.util.SequenceIdProvider
import jakarta.persistence.EntityManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.sql.Types

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
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
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
      clearRowsKeepingAiManagedMarkers(projectId, translationIds)
      return
    }
    val projectRules = evaluator.forProject(projectId)
    if (projectRules.rules.isEmpty()) {
      clearRowsKeepingAiManagedMarkers(projectId, translationIds)
      return
    }
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id

    val chunks =
      translationIds.chunked(IN_CLAUSE_CHUNK).map { chunk ->
        requeueChunk(chunk, projectRules, baseLanguageId)
      }
    websocketPublisher.publishAfterCommit(projectId, chunks.flatMap { it.updates })
    startJobsForReady(projectId, chunks.flatMap { it.queuedIds })
  }

  private data class RequeuedChunk(
    val queuedIds: List<Long>,
    val updates: List<AiReviewUpdate>,
  )

  private fun requeueChunk(
    chunk: List<Long>,
    projectRules: AiReviewRuleEvaluator.ProjectRules,
    baseLanguageId: Long,
  ): RequeuedChunk {
    val updates = mutableListOf<AiReviewUpdate>()
    val oldRows = reviewRepository.findAllByTranslationIdIn(chunk).associateBy { it.translation.id }
    val toQueue = mutableListOf<Translation>()
    val toKeepAiManaged = mutableListOf<Translation>()
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
      when {
        requeue -> toQueue += translation
        aiManaged -> toKeepAiManaged += translation
      }
      if (oldRow != null || requeue) {
        updates += rowUpdate(translation, newStatus(requeue, aiManaged))
      }
    }
    // The activity event fires after its transaction's last flush, so an unflushed change is lost
    // at commit. The flush also keeps the deletes ahead of the inserts into the unique column.
    if (deleted) entityManager.flush()
    insertRows(toQueue, AiReviewStatus.PENDING)
    insertRows(toKeepAiManaged, AiReviewStatus.FAILED)
    return RequeuedChunk(toQueue.map { it.id }, updates)
  }

  private fun newStatus(
    requeue: Boolean,
    aiManaged: Boolean,
  ): AiReviewStatus? {
    if (requeue) return AiReviewStatus.PENDING
    if (aiManaged) return AiReviewStatus.FAILED
    return null
  }

  private fun clearRowsKeepingAiManagedMarkers(
    projectId: Long,
    translationIds: Collection<Long>,
  ) {
    val updates = mutableListOf<AiReviewUpdate>()
    val toKeepAiManaged = mutableListOf<Translation>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllByTranslationIdIn(chunk)
      if (rows.isEmpty()) return@forEach
      val translations =
        translationRepository
          .findAllForAiReviewRuleEvaluation(rows.map { it.translation.id })
          .associateBy { it.id }
      rows.forEach { row ->
        translations[row.translation.id]?.let { translation ->
          val aiManaged = AiReviewEligibility.isAiManagedReview(translation, row)
          if (aiManaged) toKeepAiManaged += translation
          updates += rowUpdate(translation, newStatus(requeue = false, aiManaged = aiManaged))
        }
        reviewRepository.delete(row)
      }
    }
    if (updates.isEmpty()) return
    entityManager.flush()
    insertRows(toKeepAiManaged, AiReviewStatus.FAILED)
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  private fun rowUpdate(
    translation: Translation,
    status: AiReviewStatus?,
  ) = AiReviewUpdate.of(
    translation,
    status,
    score = null,
    scoreOnlyRun = false,
    lastFailed = false,
    queued = status == AiReviewStatus.PENDING,
  )

  @Transactional
  fun onSourceChanged(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    val updates = mutableListOf<AiReviewUpdate>()
    keyIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val stale =
        reviewRepository
          .findSiblingRows(chunk, baseLanguageId, STALE_ON_SOURCE_CHANGE)
          .filterNot { AiReviewEligibility.isAiManagedReview(it.translation, it) }
      if (stale.isEmpty()) return@forEach
      stale.forEach { updates += AiReviewUpdate.removed(it.translation) }
      reviewRepository.deleteAllByIdInBatch(stale.map { it.id })
    }
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  @Transactional
  fun onBaseLanguageChanged(projectId: Long) {
    reviewRepository.deleteByProjectExceptStatuses(projectId, keptStatuses = AiReviewEligibility.HUMAN_DECISIONS)
  }

  // Reads come first, writes after: a lookup between inserts would flush them one by one.
  @Transactional
  fun queueAndClearContext(
    translationIds: Collection<Long>,
    manualRunBatchJobId: Long? = null,
  ): List<AiReviewUpdate> {
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val existing =
        reviewRepository
          .findAllByTranslationIdIn(chunk)
          .map { it.translation.id }
          .toSet()
      val toQueue =
        translationRepository
          .findAllForAiReviewRuleEvaluation(chunk)
          .filter { it.id !in existing }
      insertRows(toQueue, AiReviewStatus.PENDING, manualRunBatchJobId)
      updates += toQueue.map { rowUpdate(it, AiReviewStatus.PENDING) }
      entityManager.flush()
      entityManager.clear()
    }
    return updates
  }

  // on conflict do nothing: a user edit and a run start can insert the same translation_id
  // concurrently, and the conflict must not abort the caller's transaction.
  private fun insertRows(
    translations: List<Translation>,
    status: AiReviewStatus,
    manualRunBatchJobId: Long? = null,
  ) {
    if (translations.isEmpty()) return
    val now = Timestamp(currentDateProvider.date.time)
    val sequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)
    jdbcTemplate.batchUpdate(
      """
      insert into translation_ai_review (
        id, created_at, updated_at, translation_id, text_hash, status, issues, re_review_requested,
        manual_run_batch_job_id
      ) values (?, ?, ?, ?, ?, ?, '[]'::jsonb, false, ?)
      on conflict (translation_id) do nothing
      """.trimIndent(),
      translations,
      1000,
    ) { ps, translation ->
      ps.setLong(1, sequenceIdProvider.next(ps.connection))
      ps.setTimestamp(2, now)
      ps.setTimestamp(3, now)
      ps.setLong(4, translation.id)
      ps.setString(5, AiReviewEligibility.currentTextHash(translation))
      ps.setString(6, status.name)
      ps.setObject(7, manualRunBatchJobId, Types.BIGINT)
    }
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
    startHiddenJob(projectId, AiReviewDemotionRequest(keyIds = keyIds.toList()), BatchJobType.AI_REVIEW_DEMOTION)
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
    startHiddenJob(
      projectId,
      AiReviewRequest(translationIds = translationIds, handlingStuckItems = handlingStuckItems),
      BatchJobType.AI_REVIEW,
    )
  }

  private fun startHiddenJob(
    projectId: Long,
    request: Any,
    type: BatchJobType,
  ) {
    val projectRef = entityManager.getReference(Project::class.java, projectId)
    batchJobService.startJob(
      request = request,
      project = projectRef,
      author = null,
      type = type,
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
    if (qaLanguageIds.isEmpty()) return reviewRepository.findQueuedTranslationIds(projectId, chunk)
    return reviewRepository.findQueuedTranslationIdsWithFreshQa(projectId, chunk, qaLanguageIds)
  }

  private fun findReadyProjectWide(
    projectId: Long,
    qaLanguageIds: Set<Long>,
  ): List<Long> {
    if (qaLanguageIds.isEmpty()) return reviewRepository.findQueuedTranslationIds(projectId)
    return reviewRepository.findQueuedTranslationIdsWithFreshQa(projectId, qaLanguageIds)
  }

  companion object {
    private val STALE_ON_SOURCE_CHANGE =
      setOf(
        AiReviewStatus.PENDING,
        AiReviewStatus.SCORE_ONLY_PASS,
        AiReviewStatus.SCORE_ONLY_FAIL,
        AiReviewStatus.BELOW_THRESHOLD,
        AiReviewStatus.FAILED,
        AiReviewStatus.SKIPPED_QA_ISSUES,
      )
  }
}
