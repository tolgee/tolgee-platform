package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.model.Project
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.apache.commons.codec.digest.DigestUtils.sha256Hex
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewQueueService(
  private val evaluator: AiReviewRuleEvaluator,
  private val reviewRepository: TranslationAiReviewRepository,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectQaConfigService: ProjectQaConfigService,
  private val languageService: LanguageService,
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
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
    val project = projectService.getDto(projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) {
      reviewRepository.deleteAllByTranslationIds(translationIds)
      return
    }
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id

    val queued = mutableListOf<Long>()
    translationIds.forEach { translationId ->
      val translation =
        entityManager.find(Translation::class.java, translationId) ?: return@forEach
      val oldRow = reviewRepository.findByTranslationId(translationId)
      val eligible = AiReviewEligibility.isEligible(translation, baseLanguageId, oldRow)
      val aiManaged = AiReviewEligibility.isAiManagedReview(translation, oldRow)
      oldRow?.let {
        reviewRepository.delete(it)
        // This runs in the activity event, which fires after the last flush of its transaction
        // (see BatchJobActivityFinalizer.afterActivityFlushed). An unflushed change is silently
        // lost at commit. The flush also keeps this delete ahead of the replacement insert,
        // which Hibernate would otherwise order first into the unique translation_id.
        entityManager.flush()
      }
      if (!eligible) return@forEach

      val decision = evaluator.evaluate(translation, activeOnly = aiManaged)
      if (decision !is RuleDecision.Review) return@forEach

      queue(translation)
      queued += translationId
    }
    if (queued.isNotEmpty()) entityManager.flush()
    startJobsForReady(projectId, queued)
  }

  /**
   * The scores of sibling rows refer to the old source text. APPROVED rows follow the
   * source-change settings instead, and CONFIRMED/REJECTED rows are human decisions.
   */
  @Transactional
  fun onSourceChanged(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    reviewRepository.deleteSiblingRows(keyIds, baseLanguageId, STALE_ON_SOURCE_CHANGE)
  }

  /**
   * Translations of the new base language are the source now, so they are not eligible.
   */
  @Transactional
  fun onBaseLanguageChanged(
    projectId: Long,
    newBaseLanguageId: Long,
  ) {
    reviewRepository.deleteByLanguage(newBaseLanguageId, KEPT_HUMAN_DECISIONS)
  }

  @Transactional
  fun queue(
    projectId: Long,
    translationIds: Collection<Long>,
    manualRunBatchJobId: Long? = null,
  ): Int {
    var queued = 0
    translationIds.forEach { translationId ->
      val translation = entityManager.find(Translation::class.java, translationId) ?: return@forEach
      if (reviewRepository.findByTranslationId(translationId) != null) return@forEach
      queue(translation, manualRunBatchJobId)
      queued++
    }
    if (queued > 0) entityManager.flush()
    return queued
  }

  /**
   * "Ready" = the row is claimable and QA does not hold it back: QA is off for the language, or
   * the translation's QA checks are fresh. One hidden job per call, no debounce: the batch
   * debounce would return the existing job and drop the new request's targets.
   */
  @Transactional
  fun startJobsForReady(
    projectId: Long,
    translationIds: Collection<Long>,
    handlingStuckItems: Boolean = false,
  ) {
    if (translationIds.isEmpty()) return
    val project = projectService.getDto(projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) return

    val ready = findReady(projectId, translationIds, isQaOn(project))
    startJob(projectId, ready, handlingStuckItems)
  }

  /**
   * Covers a pod crash during an LLM call: stale claims are released and one hidden self-heal job
   * reviews what is still queued. Skipped while another AI review job runs — that job's own
   * finalize triggers the next self-heal.
   */
  @Transactional
  fun selfHeal(projectId: Long) {
    val project = projectService.getDto(projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) return
    if (batchJobService.hasActiveJobForProject(projectId, BatchJobType.AI_REVIEW)) return
    reviewRepository.releaseStaleClaims(projectId)
    val ready = findReadyProjectWide(projectId, isQaOn(project))
    startJob(projectId, ready, handlingStuckItems = true)
  }

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

  private fun isQaOn(project: io.tolgee.dtos.cacheable.ProjectDto): Boolean =
    project.useQaChecks && projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)

  private fun findReadyProjectWide(
    projectId: Long,
    qaOn: Boolean,
  ): List<Long> {
    if (!qaOn) return reviewRepository.findClaimableTranslationIds(projectId)
    val qaLanguageIds = projectQaConfigService.getEnabledLanguageIds(projectId)
    if (qaLanguageIds.isEmpty()) return reviewRepository.findClaimableTranslationIds(projectId)
    return reviewRepository.findReadyTranslationIds(projectId, qaLanguageIds)
  }

  private fun findReady(
    projectId: Long,
    translationIds: Collection<Long>,
    qaOn: Boolean,
  ): List<Long> {
    if (!qaOn) return reviewRepository.findClaimableTranslationIds(projectId, translationIds)
    val qaLanguageIds = projectQaConfigService.getEnabledLanguageIds(projectId)
    if (qaLanguageIds.isEmpty()) return reviewRepository.findClaimableTranslationIds(projectId, translationIds)
    return reviewRepository.findReadyTranslationIds(projectId, translationIds, qaLanguageIds)
  }

  private fun queue(
    translation: Translation,
    manualRunBatchJobId: Long? = null,
  ) {
    reviewRepository.save(
      TranslationAiReview(translation).apply {
        textHash = sha256Hex(translation.text ?: "")
        status = AiReviewStatus.PENDING
        this.manualRunBatchJobId = manualRunBatchJobId
      },
    )
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

    private val KEPT_HUMAN_DECISIONS = setOf(AiReviewStatus.CONFIRMED, AiReviewStatus.REJECTED)
  }
}
