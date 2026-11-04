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
  fun markRejectedByStateChange(
    translationIds: Collection<Long>,
    resolvedById: Long?,
  ) {
    var changed = false
    translationIds.chunked(IN_CLAUSE_CHUNK).forEach { chunk ->
      reviewRepository.findAllByTranslationIdIn(chunk).forEach { row ->
        if (row.status != AiReviewStatus.APPROVED) return@forEach
        resolve(row, AiReviewStatus.REJECTED, resolvedById)
        publishRow(row)
        changed = true
      }
    }
    if (changed) entityManager.flush()
  }

  private fun publishRow(row: TranslationAiReview) {
    val translation = row.translation
    websocketPublisher.publishAfterCommit(
      translation.key.project.id,
      listOf(
        AiReviewUpdate(
          translationId = translation.id,
          keyId = translation.key.id,
          languageTag = translation.language.tag,
          status = row.status,
          score = row.score,
          dryRun = false,
          lastFailed = row.lastFailureReason != null,
        ),
      ),
    )
  }

  private fun resolveApproval(
    translation: Translation,
    userId: Long,
    state: TranslationState,
    status: AiReviewStatus,
  ): TranslationAiReview {
    val row = reviewRepository.findByTranslationId(translation.id) ?: throw NotFoundException()
    if (row.status != AiReviewStatus.APPROVED ||
      !AiReviewEligibility.isReviewForCurrentText(translation, row)
    ) {
      throw BadRequestException(Message.AI_REVIEW_NOT_AN_APPROVAL)
    }
    translationService.setStateBatch(translation, state)
    resolve(row, status, userId)
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
