package io.tolgee.service.aiReview

import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.aiReview.AiReviewStatus

/**
 * A row waits for a review when it has no result yet or a re-review was requested. The Kotlin, JPQL
 * and native SQL forms must agree, or a requested re-review is never picked up by some paths.
 */
object AiReviewQueued {
  /** Needs the alias `r` for TranslationAiReview. */
  const val JPQL = "(r.status = io.tolgee.model.enums.aiReview.AiReviewStatus.PENDING or r.reReviewRequested = true)"

  /** Needs the alias `r` for the translation_ai_review table. */
  const val SQL = "(r.status = 'PENDING' or r.re_review_requested)"

  fun of(review: TranslationAiReview): Boolean = review.status == AiReviewStatus.PENDING || review.reReviewRequested
}
