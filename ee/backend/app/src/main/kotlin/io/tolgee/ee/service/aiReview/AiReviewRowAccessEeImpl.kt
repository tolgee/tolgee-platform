package io.tolgee.ee.service.aiReview

import io.tolgee.model.translation.Translation
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.aiReview.AiReviewRowAccess
import io.tolgee.service.aiReview.AiReviewRowState
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class AiReviewRowAccessEeImpl(
  private val reviewRepository: TranslationAiReviewRepository,
) : AiReviewRowAccess {
  override fun isAiManagedReview(translation: Translation): Boolean =
    AiReviewEligibility.isAiManagedReview(translation, reviewRepository.findByTranslationId(translation.id))

  override fun hasCurrentApproval(translation: Translation): Boolean {
    val row = reviewRepository.findByTranslationId(translation.id) ?: return false
    return AiReviewEligibility.isAiApproval(translation, row)
  }

  override fun reviewStates(translations: Collection<Translation>): Map<Long, AiReviewRowState> {
    if (translations.isEmpty()) return emptyMap()
    val rows =
      reviewRepository
        .findAllByTranslationIdIn(translations.map { it.id })
        .associateBy { it.translation.id }
    return translations.associate { translation ->
      val row = rows[translation.id]
      translation.id to
        AiReviewRowState(
          aiManaged = AiReviewEligibility.isAiManagedReview(translation, row),
          currentApproval = AiReviewEligibility.isAiApproval(translation, row),
        )
    }
  }

  override fun deleteRow(translationId: Long) {
    reviewRepository.findByTranslationId(translationId)?.let { reviewRepository.delete(it) }
  }
}
