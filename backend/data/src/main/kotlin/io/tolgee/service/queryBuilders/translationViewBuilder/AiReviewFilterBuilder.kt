package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.model.Language_
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.aiReview.TranslationAiReview_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFilterOutcome
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.service.aiReview.AiReviewResultApplies
import io.tolgee.service.aiReview.AiReviewTextHash
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery

/** Builds the predicate for the `filterAiReviewStatus` / `filterAiReviewInLang` filters. */
internal class AiReviewFilterBuilder(
  private val filtering: QueryTranslationFiltering,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val isCountQuery: Boolean,
) {
  companion object {
    private val STATUS_OUTCOMES =
      mapOf(
        AiReviewFilterOutcome.REVIEWED_BY_AI to AiReviewStatus.APPROVED,
        AiReviewFilterOutcome.BELOW_THRESHOLD to AiReviewStatus.BELOW_THRESHOLD,
        AiReviewFilterOutcome.DRY_RUN_PASS to AiReviewStatus.DRY_RUN_PASS,
        AiReviewFilterOutcome.DRY_RUN_FAIL to AiReviewStatus.DRY_RUN_FAIL,
        AiReviewFilterOutcome.FAILED to AiReviewStatus.FAILED,
        AiReviewFilterOutcome.CONFIRMED to AiReviewStatus.CONFIRMED,
        AiReviewFilterOutcome.REJECTED to AiReviewStatus.REJECTED,
      )

    private val QUEUED_STATUSES = listOf(AiReviewStatus.PENDING, AiReviewStatus.SKIPPED_QA_ISSUES)
  }

  fun build(
    outcomes: List<AiReviewFilterOutcome>,
    langIds: List<Long>,
    data: AiReviewFilterData,
  ): Predicate {
    val nonBaseLangIds = langIds.filter { it != data.baseLanguageId }
    val conditions = mutableListOf<Predicate>()
    if (nonBaseLangIds.isNotEmpty()) {
      statusOutcomePredicate(outcomes, nonBaseLangIds)?.let { conditions.add(it) }
      if (AiReviewFilterOutcome.NOT_REVIEWED_YET in outcomes) {
        notReviewedYetPredicate(nonBaseLangIds, data)?.let { conditions.add(it) }
      }
    }
    val matchNoKeys = cb.disjunction()
    if (conditions.isEmpty()) return matchNoKeys
    return cb.or(*conditions.toTypedArray())
  }

  private fun statusOutcomePredicate(
    outcomes: List<AiReviewFilterOutcome>,
    langIds: List<Long>,
  ): Predicate? {
    val statuses = outcomes.mapNotNull { STATUS_OUTCOMES[it] }
    if (statuses.isEmpty()) return null
    return filtering.buildExistsOrIn(
      entityClass = TranslationAiReview::class.java,
      keyIdInSub = { r -> r.get(TranslationAiReview_.translation).get(Translation_.key).get(Key_.id) },
      languageIdInSub = { r ->
        r.get(TranslationAiReview_.translation).get(Translation_.language).get(Language_.id)
      },
      languageIds = langIds,
      extraConditions = { r ->
        val translation = r.get(TranslationAiReview_.translation)
        listOf(
          cb.or(
            *statuses
              .map { AiReviewResultApplies.criteria(cb, r.get(TranslationAiReview_.status), translation, it) }
              .toTypedArray(),
          ),
          rowIsForCurrentText(r.get(TranslationAiReview_.textHash), translation.get(Translation_.text)),
        )
      },
    )
  }

  private fun notReviewedYetPredicate(
    langIds: List<Long>,
    data: AiReviewFilterData,
  ): Predicate? {
    // Built by hand instead of through buildExistsOrIn: the rule scope and row subqueries must be
    // created from THIS subquery, or their correlation to the translation root is lost.
    val subquery = queryBase.query.subquery(Long::class.java)
    val t = subquery.from(Translation::class.java)

    val coveredByRules = perLanguageScopePredicates(langIds, data, subquery, t)
    if (coveredByRules.isEmpty()) return null

    val conditions = mutableListOf<Predicate>()
    // Same text and state conditions as AiReviewBacklogQuery.CANDIDATES and AiReviewEligibility.isEligible.
    conditions.add(cb.not(t.get(Translation_.state).`in`(TranslationState.DISABLED, TranslationState.REVIEWED)))
    conditions.add(cb.isFalse(t.get(Translation_.outdated)))
    conditions.add(
      cb.isTrue(cb.function("textregexeq", Boolean::class.java, t.get(Translation_.text), cb.literal("\\S"))),
    )
    conditions.add(cb.or(*coveredByRules.toTypedArray()))
    conditions.add(cb.not(cb.exists(currentResultRowSubquery(subquery, t))))

    val keyIdExpr = t.get(Translation_.key).get(Key_.id)
    if (!isCountQuery) {
      conditions.add(cb.equal(keyIdExpr, queryBase.root.get(Key_.id)))
    }
    subquery.select(selection(keyIdExpr))
    subquery.where(*conditions.toTypedArray())
    return filtering.wrapExistsOrIn(subquery)
  }

  private fun selection(keyIdExpr: Expression<Long>): Expression<Long> {
    if (isCountQuery) return keyIdExpr
    return cb.literal(1L)
  }

  private fun perLanguageScopePredicates(
    langIds: List<Long>,
    data: AiReviewFilterData,
    subquery: Subquery<Long>,
    t: Root<Translation>,
  ): List<Predicate> {
    val predicates = AiReviewRuleSqlPredicates(cb, subquery, data)
    return langIds.mapNotNull { languageId ->
      val (nevers, positives) =
        data.rules
          .filter { AiReviewRuleSqlPredicates.coversLanguage(it, languageId) }
          .partition { AiReviewRuleSqlPredicates.effectiveNever(it, languageId) }
      if (positives.isEmpty()) return@mapNotNull null
      val parts = mutableListOf<Predicate>()
      parts.add(cb.equal(t.get(Translation_.language).get(Language_.id), languageId))
      parts.add(cb.or(*positives.map { predicates.scopePredicate(it, t) }.toTypedArray()))
      nevers.forEach { parts.add(cb.not(predicates.scopePredicate(it, t))) }
      cb.and(*parts.toTypedArray())
    }
  }

  private fun currentResultRowSubquery(
    subquery: Subquery<Long>,
    t: Root<Translation>,
  ): Subquery<Long> {
    val resultRowSub = subquery.subquery(Long::class.java)
    val row = resultRowSub.from(TranslationAiReview::class.java)
    resultRowSub.select(cb.literal(1L))
    resultRowSub.where(
      cb.equal(row.get(TranslationAiReview_.translation).get(Translation_.id), t.get(Translation_.id)),
      cb.not(row.get(TranslationAiReview_.status).`in`(QUEUED_STATUSES)),
      rowIsForCurrentText(row.get(TranslationAiReview_.textHash), t.get(Translation_.text)),
    )
    return resultRowSub
  }

  private fun rowIsForCurrentText(
    textHash: Expression<String>,
    text: Expression<String?>,
  ): Predicate = cb.equal(textHash, AiReviewTextHash.criteria(cb, text))
}
