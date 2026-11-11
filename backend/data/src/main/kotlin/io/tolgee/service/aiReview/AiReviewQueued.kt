package io.tolgee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus

/** The Kotlin, JPQL and native SQL forms must agree, or some paths never pick a queued row up. */
object AiReviewQueued {
  const val JPQL = "(r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING or r.reReviewRequested = true)"

  const val SQL = "(r.status = 'PENDING' or r.re_review_requested)"

  fun of(review: TranslationAiReview): Boolean = review.status == AiReviewStatus.PENDING || review.reReviewRequested
}
