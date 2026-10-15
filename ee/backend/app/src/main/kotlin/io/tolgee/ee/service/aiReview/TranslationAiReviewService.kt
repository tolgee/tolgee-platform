package io.tolgee.ee.service.aiReview

import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.aiReviewTextHash
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationAiReviewService(
  private val translationAiReviewRepository: TranslationAiReviewRepository,
  private val translationService: TranslationService,
  private val securityService: SecurityService,
  private val currentDateProvider: CurrentDateProvider,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val triggerService: AiReviewTriggerService,
) {
  fun getDetail(translation: Translation): TranslationAiReview {
    val review =
      translationAiReviewRepository.findByTranslationId(translation.id)
        ?: throw NotFoundException(Message.AI_REVIEW_NOT_FOUND)
    if (
      review.outcome == AiReviewOutcome.PENDING ||
      review.outcome.isObsoleteFor(translation.state) ||
      review.textHash != aiReviewTextHash(translation.text)
    ) {
      throw NotFoundException(Message.AI_REVIEW_NOT_FOUND)
    }
    return review
  }

  @Transactional
  fun confirm(
    projectId: Long,
    translationId: Long,
    user: UserAccount,
  ): TranslationAiReview {
    val translation = translationService.get(projectId, translationId)
    checkCanResolve(translation)
    val review = getAutoReviewedReview(translation)
    review.outcome = AiReviewOutcome.CONFIRMED
    review.resolvedBy = user
    review.resolvedAt = currentDateProvider.date
    translationAiReviewRepository.save(review)
    publishResolved(projectId, translation, review)
    return review
  }

  @Transactional
  fun reject(
    projectId: Long,
    translationId: Long,
    user: UserAccount,
  ): Translation {
    val translation = translationService.get(projectId, translationId)
    checkCanResolve(translation)
    val review = getAutoReviewedReview(translation)
    review.outcome = AiReviewOutcome.REJECTED
    review.resolvedBy = user
    review.resolvedAt = currentDateProvider.date
    translationAiReviewRepository.save(review)
    translationService.setStateBatch(translation, TranslationState.TRANSLATED)
    publishResolved(projectId, translation, review)
    return translation
  }

  @Transactional
  fun retry(
    projectId: Long,
    translationId: Long,
  ) {
    val translation = translationService.get(projectId, translationId)
    checkCanResolve(translation)
    val review = getDetail(translation)
    if (review.outcome != AiReviewOutcome.FAILED) {
      throw BadRequestException(Message.AI_REVIEW_CANNOT_BE_RETRIED)
    }
    val change =
      AiReviewTriggerService.TextChange(
        translationId = translation.id,
        keyId = translation.key.id,
        languageId = translation.language.id,
        newText = translation.text,
      )
    val targets = triggerService.createPending(projectId, listOf(change))
    if (targets.isEmpty()) {
      triggerService.invalidate(listOf(translation.id))
    }
    applicationEventPublisher.publishEvent(
      AiReviewUpdatedEvent.of(projectId, translation, review).copy(outcome = AiReviewOutcome.PENDING),
    )
    triggerService.startReview(projectId, targets)
  }

  private fun checkCanResolve(translation: Translation) {
    securityService.checkStateChangePermission(translation)
    securityService.checkBranchModify(translation)
  }

  private fun getAutoReviewedReview(translation: Translation): TranslationAiReview {
    val review = getDetail(translation)
    if (review.outcome != AiReviewOutcome.AUTO_REVIEWED) {
      throw BadRequestException(Message.AI_REVIEW_CANNOT_BE_RESOLVED)
    }
    return review
  }

  private fun publishResolved(
    projectId: Long,
    translation: Translation,
    review: TranslationAiReview,
  ) {
    applicationEventPublisher.publishEvent(AiReviewUpdatedEvent.of(projectId, translation, review))
  }
}
