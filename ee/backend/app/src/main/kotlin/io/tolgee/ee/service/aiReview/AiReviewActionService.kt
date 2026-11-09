package io.tolgee.ee.service.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewActionService(
  private val reviewRepository: TranslationAiReviewRepository,
  private val ruleRepository: AiReviewRuleRepository,
  private val translationService: TranslationService,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
  private val websocketPublisher: AiReviewWebsocketPublisher,
) {
  @Transactional
  fun confirm(
    translation: Translation,
    userId: Long,
  ): TranslationAiReview = resolveApproval(translation, userId, TranslationState.REVIEWED, AiReviewStatus.CONFIRMED)

  @Transactional
  fun reject(
    translation: Translation,
    userId: Long,
  ): TranslationAiReview = resolveApproval(translation, userId, TranslationState.TRANSLATED, AiReviewStatus.REJECTED)

  fun get(translationId: Long): TranslationAiReview =
    reviewRepository.findByTranslationId(translationId) ?: throw NotFoundException()

  @Transactional
  fun settleHumanUnReview(
    projectId: Long,
    translationIds: Collection<Long>,
    textChangedIds: Set<Long>,
    resolvedById: Long?,
    aiReviewOn: Boolean,
  ) {
    val updates = mutableListOf<AiReviewUpdate>()
    // Sorted: parallel chunks of one bulk job must lock the rule rows in the same order, or they deadlock.
    val rejectedByRule = sortedMapOf<Long, Long>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllWithTranslationByTranslationIdIn(chunk)
      val (deletedApprovals, rejectedApprovals) =
        rows
          .filter { it.status == AiReviewStatus.APPROVED }
          .partition { !aiReviewOn || isMachineOverwrite(it, textChangedIds) }
      rejectedApprovals.forEach { row ->
        resolve(row, AiReviewStatus.REJECTED, resolvedById)
        updates += AiReviewUpdate.of(row, dryRun = false)
        row.rule?.id?.let { rejectedByRule.merge(it, 1L, Long::plus) }
      }
      deleteAndCollectRemovals(rows.filter { AiReviewEligibility.isMarker(it) } + deletedApprovals, updates)
    }
    websocketPublisher.publishAfterCommit(projectId, updates)
    if (rejectedByRule.isEmpty()) return
    entityManager.flush()
    rejectedByRule.forEach { (ruleId, count) -> ruleRepository.addToRejectedCount(ruleId, count) }
  }

  private fun isMachineOverwrite(
    row: TranslationAiReview,
    textChangedIds: Set<Long>,
  ): Boolean = row.translation.id in textChangedIds && row.translation.auto

  @Transactional
  fun releaseToHumanReview(
    projectId: Long,
    translationIds: Collection<Long>,
  ) {
    val updates = mutableListOf<AiReviewUpdate>()
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      val rows = reviewRepository.findAllWithTranslationByTranslationIdIn(chunk)
      deleteAndCollectRemovals(rows.filter { AiReviewEligibility.keepsAiManaged(it) }, updates)
    }
    websocketPublisher.publishAfterCommit(projectId, updates)
  }

  private fun deleteAndCollectRemovals(
    rows: List<TranslationAiReview>,
    updates: MutableList<AiReviewUpdate>,
  ) {
    if (rows.isEmpty()) return
    rows.forEach { updates += AiReviewUpdate.removed(it.translation) }
    reviewRepository.deleteAllByIdInBatch(rows.map { it.id })
  }

  private fun publishRow(row: TranslationAiReview) {
    websocketPublisher.publishAfterCommit(
      row.translation.key.project.id,
      listOf(AiReviewUpdate.of(row, dryRun = false)),
    )
  }

  private fun resolveApproval(
    translation: Translation,
    userId: Long,
    state: TranslationState,
    status: AiReviewStatus,
  ): TranslationAiReview {
    val row = get(translation.id)
    if (!AiReviewEligibility.isAiApproval(translation, row)) {
      throw BadRequestException(Message.AI_REVIEW_NOT_AN_APPROVAL)
    }
    translationService.setStateBatch(translation, state)
    resolve(row, status, userId)
    if (status == AiReviewStatus.REJECTED) row.rule?.id?.let { ruleRepository.addToRejectedCount(it, 1) }
    publishRow(row)
    return row
  }

  private fun resolve(
    row: TranslationAiReview,
    status: AiReviewStatus,
    resolvedById: Long?,
  ) {
    row.status = status
    row.resolvedBy = resolvedById?.let { entityManager.getReference(UserAccount::class.java, it) }
    row.resolvedAt = currentDateProvider.date
    row.reReviewRequested = false
  }
}
