package io.tolgee.ee.service.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewActionService(
  private val reviewRepository: TranslationAiReviewRepository,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
  private val websocketPublisher: AiReviewWebsocketPublisher,
) {
  fun get(translationId: Long): TranslationAiReview =
    reviewRepository.findByTranslationId(translationId) ?: throw NotFoundException()

  fun checkRetryAllowed(translationId: Long) {
    val row = reviewRepository.findByTranslationId(translationId) ?: return
    if (row.status == AiReviewStatus.OVERRIDDEN) {
      throw BadRequestException(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    }
  }

  @Transactional
  fun settleHumanUnReview(
    projectId: Long,
    translationIds: Collection<Long>,
    textChangedIds: Set<Long>,
    resolvedById: Long?,
    aiReviewOn: Boolean,
  ) {
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllWithTranslationByTranslationIdIn(chunk)
      val (deletedApprovals, overriddenApprovals) =
        rows
          .filter { it.status == AiReviewStatus.APPROVED }
          .partition { !aiReviewOn || isMachineOverwrite(it, textChangedIds) }
      updates += markOverridden(overriddenApprovals, resolvedById)
      updates += deleteRows(rows.filter { AiReviewEligibility.isMarker(it) } + deletedApprovals)
    }
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  private fun isMachineOverwrite(
    row: TranslationAiReview,
    textChangedIds: Set<Long>,
  ): Boolean = row.translation.id in textChangedIds && row.translation.auto

  @Transactional
  fun releaseToHumanReview(
    projectId: Long,
    translationIds: Collection<Long>,
    resolvedById: Long?,
    aiReviewOn: Boolean,
  ) {
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllWithTranslationByTranslationIdIn(chunk)
      val (toOverride, rest) = rows.partition { aiReviewOn && overridesOnHumanReview(it) }
      updates += markOverridden(toOverride, resolvedById)
      updates += deleteRows(rest.filter { AiReviewEligibility.keepsAiManaged(it) })
    }
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  private fun overridesOnHumanReview(row: TranslationAiReview): Boolean =
    row.status in OVERRIDABLE_RESULTS && AiReviewEligibility.isReviewForCurrentText(row.translation, row)

  private fun deleteRows(rows: List<TranslationAiReview>): List<AiReviewUpdate> {
    if (rows.isEmpty()) return emptyList()
    val removals = rows.map { AiReviewUpdate.removed(it.translation) }
    reviewRepository.deleteAllByIdInBatch(rows.map { it.id })
    return removals
  }

  private fun markOverridden(
    rows: List<TranslationAiReview>,
    resolvedById: Long?,
  ): List<AiReviewUpdate> =
    rows.map { row ->
      row.status = AiReviewStatus.OVERRIDDEN
      row.resolvedBy = resolvedById?.let { entityManager.getReference(UserAccount::class.java, it) }
      row.resolvedAt = currentDateProvider.date
      row.reReviewRequested = false
      // An overridden row must never show Retry, so the stale failure data goes with the decision.
      row.lastFailureReason = null
      row.lastFailedAt = null
      AiReviewUpdate.of(row, scoreOnlyRun = false)
    }

  companion object {
    private val OVERRIDABLE_RESULTS =
      setOf(AiReviewStatus.BELOW_THRESHOLD, AiReviewStatus.SCORE_ONLY_PASS, AiReviewStatus.SCORE_ONLY_FAIL)
  }
}
