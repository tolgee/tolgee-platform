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
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewActionService(
  private val reviewRepository: TranslationAiReviewRepository,
  private val translationService: TranslationService,
  private val currentDateProvider: CurrentDateProvider,
  private val entityManager: EntityManager,
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

  /**
   * Any human un-review of an AI approval counts as a rejection, so the rejected metric stays
   * complete. The system's own demotions never arrive here.
   */
  @Transactional
  fun markRejectedByStateChange(
    translationIds: Collection<Long>,
    resolvedById: Long?,
  ) {
    var changed = false
    translationIds.forEach { translationId ->
      val row = reviewRepository.findByTranslationId(translationId) ?: return@forEach
      if (row.status != AiReviewStatus.APPROVED) return@forEach
      resolve(row, AiReviewStatus.REJECTED, resolvedById)
      changed = true
    }
    // The activity event fires after the last flush of its transaction, so pending changes would
    // be silently lost at commit.
    if (changed) entityManager.flush()
  }

  private fun resolveApproval(
    translation: Translation,
    userId: Long,
    state: TranslationState,
    status: AiReviewStatus,
  ): TranslationAiReview {
    val row = reviewRepository.findByTranslationId(translation.id) ?: throw NotFoundException()
    if (row.status != AiReviewStatus.APPROVED) {
      throw BadRequestException(Message.AI_REVIEW_NOT_AN_APPROVAL)
    }
    translationService.setStateBatch(translation, state)
    resolve(row, status, userId)
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
