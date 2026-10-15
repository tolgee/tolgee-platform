package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.Language_
import io.tolgee.model.Project_
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.AiReviewRule_
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.aiReview.TranslationAiReview_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewFilterOutcome
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.Tag_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.util.aiReviewTextHash
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.From
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Subquery

class AiReviewFilterContext(
  val enabledRules: List<AiReviewRule>,
  val baseLanguageId: Long,
)

/**
 * Unlike the other translation-level filters, which are OR-ed together, the AI review group is
 * AND-ed with the rest of the query.
 */
internal class AiReviewFilterBuilder(
  private val params: TranslationFilters,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val projectId: Long,
  private val context: AiReviewFilterContext,
  private val isCountQuery: Boolean,
) {
  fun build(): Predicate? {
    if (!params.hasAiReviewFilter()) return null
    val outcomes = params.filterAiReviewOutcome.orEmpty().toSet()
    val scoreBelow = params.filterAiReviewScoreBelow
    val ruleIds = params.filterAiReviewRuleId.orEmpty()

    val languageIds = resolveLanguageIds()
    if (languageIds.isEmpty()) return cb.disjunction()

    val resultOutcomes = outcomes - AiReviewFilterOutcome.NOT_REVIEWED
    val predicates = mutableListOf<Predicate>()
    if (resultOutcomes.isNotEmpty() || outcomes.isEmpty()) {
      predicates.add(buildResultExists(languageIds, resultOutcomes.ifEmpty { RESULT_OUTCOMES }, scoreBelow, ruleIds))
    }
    if (AiReviewFilterOutcome.NOT_REVIEWED in outcomes && scoreBelow == null) {
      buildNotReviewedExists(languageIds, ruleIds)?.let { predicates.add(it) }
    }
    if (predicates.isEmpty()) return cb.disjunction()
    return cb.or(*predicates.toTypedArray())
  }

  private fun resolveLanguageIds(): List<Long> {
    val tags = params.filterAiReviewInLang
    if (tags.isNullOrEmpty()) {
      return queryBase.languages.map { it.id }.filter { it != context.baseLanguageId }
    }
    return queryBase.languages.filter { it.tag in tags }.map { it.id }
  }

  private fun buildResultExists(
    languageIds: List<Long>,
    outcomes: Set<AiReviewFilterOutcome>,
    scoreBelow: Int?,
    ruleIds: List<Long>,
  ): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val review = subquery.from(TranslationAiReview::class.java)
    val translation = review.join(TranslationAiReview_.translation)
    val key = translation.join(Translation_.key)
    val conditions =
      mutableListOf(
        cb.equal(key.get(Key_.project).get(Project_.id), projectId),
        translation.get(Translation_.language).get(Language_.id).`in`(languageIds),
        cb.equal(review.get(TranslationAiReview_.textHash), cb.aiReviewTextHash(translation.get(Translation_.text))),
        cb.or(*outcomes.map { outcomePredicate(it, review, translation) }.toTypedArray()),
      )
    scoreBelow?.let { conditions.add(cb.lessThan(review.get(TranslationAiReview_.score), it)) }
    if (ruleIds.isNotEmpty()) {
      conditions.add(review.get(TranslationAiReview_.rule).get(AiReviewRule_.id).`in`(ruleIds))
    }
    return matchesRowKey(subquery, key.get(Key_.id), conditions)
  }

  private fun outcomePredicate(
    outcome: AiReviewFilterOutcome,
    review: From<*, TranslationAiReview>,
    translation: From<*, Translation>,
  ): Predicate {
    val (stored, dryRun) =
      when (outcome) {
        AiReviewFilterOutcome.AUTO_REVIEWED -> AiReviewOutcome.AUTO_REVIEWED to null
        AiReviewFilterOutcome.WOULD_AUTO_REVIEW -> AiReviewOutcome.WOULD_AUTO_REVIEW to null
        AiReviewFilterOutcome.BELOW_THRESHOLD -> AiReviewOutcome.BELOW_THRESHOLD to false
        AiReviewFilterOutcome.WOULD_NOT_AUTO_REVIEW -> AiReviewOutcome.BELOW_THRESHOLD to true
        AiReviewFilterOutcome.FAILED -> AiReviewOutcome.FAILED to null
        AiReviewFilterOutcome.NOT_REVIEWED -> return cb.disjunction()
      }
    val conditions = mutableListOf(cb.equal(review.get(TranslationAiReview_.outcome), stored))
    dryRun?.let { conditions.add(cb.equal(review.get(TranslationAiReview_.dryRun), it)) }
    if (stored.isShownOnlyWhileTranslated) {
      conditions.add(cb.equal(translation.get(Translation_.state), TranslationState.TRANSLATED))
    }
    return cb.and(*conditions.toTypedArray())
  }

  private fun buildNotReviewedExists(
    languageIds: List<Long>,
    ruleIds: List<Long>,
  ): Predicate? {
    val candidateLanguageIds = languageIds.filter { it != context.baseLanguageId }
    val reviewingRules = context.enabledRules.filter { ruleIds.isEmpty() || it.id in ruleIds }
    if (candidateLanguageIds.isEmpty() || reviewingRules.isEmpty()) return null

    val subquery = queryBase.query.subquery(Long::class.java)
    val translation = subquery.from(Translation::class.java)
    val key = translation.join(Translation_.key)
    val namespaceName = cb.coalesce(key.join(Key_.namespace, JoinType.LEFT).get(Namespace_.name), "")
    val languageId = translation.get(Translation_.language).get(Language_.id)
    val text = translation.get(Translation_.text)

    fun rulePredicate(
      rule: AiReviewRule,
      languages: List<Long>,
    ): Predicate? {
      if (languages.isEmpty()) return null
      return cb.and(languageId.`in`(languages), ruleScopePredicate(rule, subquery, key, namespaceName))
    }

    val reviewPredicates =
      reviewingRules.mapNotNull { rule -> rulePredicate(rule, splitRuleLanguages(rule, candidateLanguageIds).reviewed) }
    if (reviewPredicates.isEmpty()) return null
    val neverPredicates =
      context.enabledRules.mapNotNull { rule ->
        rulePredicate(rule, splitRuleLanguages(rule, candidateLanguageIds).never)
      }

    val conditions =
      mutableListOf(
        cb.equal(key.get(Key_.project).get(Project_.id), projectId),
        languageId.`in`(candidateLanguageIds),
        cb.equal(translation.get(Translation_.state), TranslationState.TRANSLATED),
        cb.isNotNull(text),
        cb.notEqual(cb.trim(text), ""),
        cb.not(translation.get(Translation_.id).`in`(currentResultTranslationIds(subquery))),
        cb.or(*reviewPredicates.toTypedArray()),
      )
    if (neverPredicates.isNotEmpty()) {
      conditions.add(cb.not(cb.or(*neverPredicates.toTypedArray())))
    }
    return matchesRowKey(subquery, key.get(Key_.id), conditions)
  }

  private fun currentResultTranslationIds(parent: Subquery<Long>): Subquery<Long> {
    val subquery = parent.subquery(Long::class.java)
    val review = subquery.from(TranslationAiReview::class.java)
    val translation = review.join(TranslationAiReview_.translation)
    subquery.select(translation.get(Translation_.id))
    subquery.where(
      cb.equal(translation.get(Translation_.key).get(Key_.project).get(Project_.id), projectId),
      cb.equal(review.get(TranslationAiReview_.textHash), cb.aiReviewTextHash(translation.get(Translation_.text))),
      cb.notEqual(review.get(TranslationAiReview_.outcome), AiReviewOutcome.PENDING),
    )
    return subquery
  }

  private class RuleLanguages(
    val reviewed: List<Long>,
    val never: List<Long>,
  )

  private fun splitRuleLanguages(
    rule: AiReviewRule,
    candidateLanguageIds: List<Long>,
  ): RuleLanguages {
    val never = rule.languageThresholds.filter { it.never }.mapTo(HashSet()) { it.languageId }
    val (neverLanguages, reviewedLanguages) = getRuleLanguageIds(rule, candidateLanguageIds).partition { it in never }
    return RuleLanguages(reviewed = reviewedLanguages, never = neverLanguages)
  }

  private fun getRuleLanguageIds(
    rule: AiReviewRule,
    candidateLanguageIds: List<Long>,
  ): List<Long> {
    if (rule.languageIds.isEmpty()) return candidateLanguageIds
    return candidateLanguageIds.filter { it in rule.languageIds }
  }

  private fun ruleScopePredicate(
    rule: AiReviewRule,
    parent: Subquery<Long>,
    key: From<*, Key>,
    namespaceName: Expression<String>,
  ): Predicate {
    val keyId = key.get(Key_.id)
    val conditions = mutableListOf<Predicate>()
    val tagIn = rule.tagIn.filter { it.isNotEmpty() }
    val tagNotIn = rule.tagNotIn.filter { it.isNotEmpty() }
    if (rule.tagIn.isNotEmpty()) {
      val options = mutableListOf<Predicate>()
      if (tagIn.isNotEmpty()) options.add(keyId.`in`(keyIdsTaggedWith(parent, tagIn)))
      if (NO_TAG in rule.tagIn) options.add(cb.not(keyId.`in`(keyIdsWithAnyTag(parent))))
      conditions.add(cb.or(*options.toTypedArray()))
    }
    if (tagNotIn.isNotEmpty()) conditions.add(cb.not(keyId.`in`(keyIdsTaggedWith(parent, tagNotIn))))
    if (NO_TAG in rule.tagNotIn) conditions.add(keyId.`in`(keyIdsWithAnyTag(parent)))
    if (rule.namespaceIn.isNotEmpty()) conditions.add(namespaceName.`in`(rule.namespaceIn))
    if (rule.namespaceNotIn.isNotEmpty()) conditions.add(cb.not(namespaceName.`in`(rule.namespaceNotIn)))
    return cb.and(*conditions.toTypedArray())
  }

  private fun keyIdsWithAnyTag(parent: Subquery<Long>): Subquery<Long> = keyIdsTagged(parent, names = null)

  private fun keyIdsTaggedWith(
    parent: Subquery<Long>,
    names: List<String>,
  ): Subquery<Long> = keyIdsTagged(parent, names)

  private fun keyIdsTagged(
    parent: Subquery<Long>,
    names: List<String>?,
  ): Subquery<Long> {
    val subquery = parent.subquery(Long::class.java)
    val keyMeta = subquery.from(KeyMeta::class.java)
    val tag = keyMeta.join(KeyMeta_.tags)
    subquery.select(keyMeta.get(KeyMeta_.key).get(Key_.id))
    val conditions = mutableListOf(cb.equal(tag.get(Tag_.project).get(Project_.id), projectId))
    names?.let { conditions.add(tag.get(Tag_.name).`in`(it)) }
    subquery.where(*conditions.toTypedArray())
    return subquery
  }

  private fun matchesRowKey(
    subquery: Subquery<Long>,
    keyId: Expression<Long>,
    conditions: List<Predicate>,
  ): Predicate {
    if (isCountQuery) {
      subquery.select(keyId)
      subquery.where(*conditions.toTypedArray())
      return queryBase.root.get(Key_.id).`in`(subquery)
    }
    subquery.select(cb.literal(1L))
    subquery.where(*(conditions + cb.equal(keyId, queryBase.root.get(Key_.id))).toTypedArray())
    return cb.exists(subquery)
  }

  companion object {
    private const val NO_TAG = ""
    private val RESULT_OUTCOMES =
      setOf(
        AiReviewFilterOutcome.AUTO_REVIEWED,
        AiReviewFilterOutcome.BELOW_THRESHOLD,
        AiReviewFilterOutcome.WOULD_AUTO_REVIEW,
        AiReviewFilterOutcome.WOULD_NOT_AUTO_REVIEW,
        AiReviewFilterOutcome.FAILED,
      )
  }
}
