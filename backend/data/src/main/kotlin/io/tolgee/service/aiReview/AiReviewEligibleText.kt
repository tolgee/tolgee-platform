package io.tolgee.service.aiReview

import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate

object AiReviewEligibleText {
  // Same as the SQL `~ '\S'`: Postgres keeps NBSP as text, isNullOrBlank does not.
  private val NON_SPACE = Regex("""\S""")

  /** REVIEWED rows: see AiReviewManaged. */
  fun of(translation: Translation): Boolean =
    hasReviewableText(translation) &&
      translation.state != TranslationState.DISABLED &&
      !translation.outdated

  fun hasReviewableText(translation: Translation): Boolean = translation.text?.contains(NON_SPACE) == true

  const val SQL_NOT_REVIEWED = """t.text ~ '\S'
        and t.state <> :disabled
        and t.state <> :reviewed
        and t.outdated = false"""

  fun criteriaNotReviewed(
    cb: CriteriaBuilder,
    t: Path<Translation>,
  ): List<Predicate> =
    listOf(
      cb.not(t.get(Translation_.state).`in`(TranslationState.DISABLED, TranslationState.REVIEWED)),
      cb.isFalse(t.get(Translation_.outdated)),
      cb.isTrue(cb.function("textregexeq", Boolean::class.java, t.get(Translation_.text), cb.literal("\\S"))),
    )
}
