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
  private val websocketPublisher: AiReviewWebsocketPublisher,
) : AiReviewRowAccess {
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
    val row = reviewRepository.findByTranslationId(translationId) ?: return
    reviewRepository.delete(row)
    val translation = row.translation
    websocketPublisher.publishAfterCommit(
      translation.key.project.id,
      listOf(AiReviewUpdate.removed(translation)),
    )
  }
}
