package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilterByLabel
import io.tolgee.dtos.request.translation.TranslationFilterByState
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.Language_
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.TranslationSuggestion_
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.key.Key_
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.qa.TranslationQaIssue_
import io.tolgee.model.translation.Label_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.model.translation.TranslationComment_
import io.tolgee.model.translation.Translation_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery

/**
 * Translation-level filter builder for the Translation View query.
 *
 * All filters here are expressed as `EXISTS` / `NOT EXISTS` subqueries (data queries) or
 * `k.id IN (…)` / `NOT IN (…)` (count queries) on the `Translation`, `TranslationComment`,
 * `TranslationSuggestion`, or `TranslationQaIssue` tables. The main query never joins those
 * tables directly, so every filter stands on its own.
 *
 * **Multi-language collapse**: when the same per-language EXISTS predicate would apply to
 * several languages, the N subqueries are collapsed into one keyed on `language_id IN (…)`.
 * Does NOT apply to `NOT EXISTS` filters (`filterHasNoSuggestionsInLang`) or to `filterLabel`
 * (different label-id sets per language). `filterState` has its own more elaborate collapse —
 * see [StateFilterBuilder].
 *
 * **OR semantics**: every filter method adds one (or more) predicates to
 * [QueryBase.translationConditions]. These are OR-ed together at query build time in
 * [TranslationsViewQueryBuilder.getWhereConditions].
 */
class QueryTranslationFiltering(
  private val params: TranslationFilters,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val isCountQuery: Boolean = false,
) {
  companion object {
    private const val LABEL_IN_CLAUSE_CHUNK_SIZE = 10_000
  }

  private val stateFilterBuilder = StateFilterBuilder(queryBase, cb, isCountQuery)

  fun applyStateFilters() {
    val byStateMap = filterByStateMap ?: return
    if (byStateMap.isEmpty()) return
    val perLanguage =
      byStateMap.mapNotNull { (tag, states) ->
        val language = queryBase.languages.find { it.tag == tag } ?: return@mapNotNull null
        language to states.toSet()
      }
    val predicate = stateFilterBuilder.build(perLanguage) ?: return
    queryBase.translationConditions.add(predicate)
  }

  /** `filterAutoTranslatedInLang` — collapsed to one subquery with `language_id IN (…)`. */
  fun applyAutoTranslatedFilter() {
    val langIds = languageIdsForTags(params.filterAutoTranslatedInLang) ?: return
    queryBase.translationConditions.add(
      buildTranslationExists(langIds) { t -> cb.isTrue(t.get(Translation_.auto)) },
    )
  }

  /** `filterUntranslatedInLang` — single-language `NOT EXISTS` of a non-empty row. */
  fun applyUntranslatedInLangFilter() {
    val tag = params.filterUntranslatedInLang ?: return
    val language = queryBase.languages.find { it.tag == tag } ?: return
    queryBase.translationConditions.add(
      cb.not(
        buildTranslationExists(listOf(language.id)) { t ->
          cb.and(cb.isNotNull(t.get(Translation_.text)), cb.notEqual(t.get(Translation_.text), ""))
        },
      ),
    )
  }

  /** `filterTranslatedInLang` — single-language `EXISTS` of a non-empty row. */
  fun applyTranslatedInLangFilter() {
    val tag = params.filterTranslatedInLang ?: return
    val language = queryBase.languages.find { it.tag == tag } ?: return
    queryBase.translationConditions.add(
      buildTranslationExists(listOf(language.id)) { t ->
        cb.and(cb.isNotNull(t.get(Translation_.text)), cb.notEqual(t.get(Translation_.text), ""))
      },
    )
  }

  /** `filterHasCommentsInLang` — collapsed, over `TranslationComment`. */
  fun applyHasCommentsFilter() {
    val langIds = languageIdsForTags(params.filterHasCommentsInLang) ?: return
    queryBase.translationConditions.add(buildCommentExists(langIds, unresolvedOnly = false))
  }

  /** `filterHasUnresolvedCommentsInLang` — collapsed, `state = NEEDS_RESOLUTION`. */
  fun applyHasUnresolvedCommentsFilter() {
    val langIds = languageIdsForTags(params.filterHasUnresolvedCommentsInLang) ?: return
    queryBase.translationConditions.add(buildCommentExists(langIds, unresolvedOnly = true))
  }

  /** `filterHasSuggestionsInLang` — collapsed, over `TranslationSuggestion`, `state = ACTIVE`. */
  fun applyHasSuggestionsFilter() {
    val langIds = languageIdsForTags(params.filterHasSuggestionsInLang) ?: return
    queryBase.translationConditions.add(buildActiveSuggestionExists(langIds))
  }

  /**
   * `filterHasNoSuggestionsInLang` — not collapsed: `NOT EXISTS (lang=L1) OR NOT EXISTS
   * (lang=L2)` ≠ `NOT EXISTS (lang IN (L1, L2))`. One `NOT EXISTS` per language, OR-ed.
   */
  fun applyHasNoSuggestionsFilter() {
    val langs = params.filterHasNoSuggestionsInLang ?: return
    if (langs.isEmpty()) return
    val perLangPredicates =
      langs.mapNotNull { tag ->
        val language = queryBase.languages.find { it.tag == tag } ?: return@mapNotNull null
        cb.not(buildActiveSuggestionExists(listOf(language.id)))
      }
    if (perLangPredicates.isEmpty()) return
    queryBase.translationConditions.add(cb.or(*perLangPredicates.toTypedArray()))
  }

  /**
   * `filterLabel` — not collapsed (different label-id sets may be requested per language).
   * One EXISTS subquery per language group in the parsed map, OR-ed.
   */
  fun applyLabelFilter() {
    val map = filterByLabelMap ?: return
    if (map.isEmpty()) return
    val perLangPredicates =
      map.mapNotNull { (tag, labelIds) ->
        if (labelIds.isEmpty()) return@mapNotNull null
        val language = queryBase.languages.find { it.tag == tag } ?: return@mapNotNull null
        buildLabelExists(language, labelIds.map { it.labelId })
      }
    if (perLangPredicates.isEmpty()) return
    queryBase.translationConditions.add(cb.or(*perLangPredicates.toTypedArray()))
  }

  /**
   * QA filters — `filterHasQaIssuesInLang` (collapsed across requested langs) and
   * `filterQaCheckType` (collapsed across all selected return langs).
   */
  fun applyQaFilters() {
    val hasIssuesLangIds = languageIdsForTags(params.filterHasQaIssuesInLang)
    if (hasIssuesLangIds != null) {
      queryBase.translationConditions.add(buildQaIssueExists(hasIssuesLangIds, checkTypes = null))
    }
    val checkTypes = params.filterQaCheckType?.takeIf { it.isNotEmpty() }
    if (checkTypes != null) {
      val allLangIds = queryBase.languages.map { it.id }
      if (allLangIds.isNotEmpty()) {
        queryBase.translationConditions.add(buildQaIssueExists(allLangIds, checkTypes = checkTypes))
      }
    }
  }

  /**
   * `filterOutdatedLanguage` / `filterNotOutdatedLanguage` — each collapsed to one subquery.
   */
  fun applyOutdatedFilters() {
    val outdatedLangIds = languageIdsForTags(params.filterOutdatedLanguage)
    val notOutdatedLangIds = languageIdsForTags(params.filterNotOutdatedLanguage)
    if (outdatedLangIds == null && notOutdatedLangIds == null) return

    val conditions = mutableListOf<Predicate>()
    if (outdatedLangIds != null) {
      conditions.add(buildTranslationExists(outdatedLangIds) { t -> cb.isTrue(t.get(Translation_.outdated)) })
    }
    if (notOutdatedLangIds != null) {
      conditions.add(buildTranslationExists(notOutdatedLangIds) { t -> cb.isFalse(t.get(Translation_.outdated)) })
    }
    if (conditions.isNotEmpty()) {
      queryBase.translationConditions.add(cb.or(*conditions.toTypedArray()))
    }
  }

  // ─── generic subquery helpers ───────────────────────────────────────────

  /**
   * Builds an "exists a row for this key" subquery over [entityClass], then wraps it as a
   * correlated `EXISTS` (data query) or non-correlated `k.id IN (…)` (count query) via
   * [wrapExistsOrIn]. The single shared helper replaces what used to be four near-identical
   * `buildXxxExistsMultiLang` methods — each filter method now just supplies the right entity
   * class, paths, and conditions.
   *
   * - [keyIdInSub]: how to reach `key.id` from the subquery root (e.g. `t.key.id` on
   *   `Translation`, `tc.translation.key.id` on `TranslationComment`).
   * - [languageIdInSub]: how to reach `language.id` from the subquery root.
   * - [languageIds]: emitted as `= L` for a single id, `IN (…)` for many.
   * - [extraConditions]: any predicates specific to the filter (state, outdated, check type, …).
   */
  private fun <E> buildExistsOrIn(
    entityClass: Class<E>,
    keyIdInSub: (Root<E>) -> Expression<Long>,
    languageIdInSub: (Root<E>) -> Path<Long>,
    languageIds: List<Long>,
    extraConditions: (Root<E>) -> List<Predicate> = { emptyList() },
  ): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val root = subquery.from(entityClass)
    val keyIdExpr = keyIdInSub(root)
    val conditions = mutableListOf<Predicate>()
    conditions.add(languagePredicate(languageIdInSub(root), languageIds))
    conditions.addAll(extraConditions(root))
    if (!isCountQuery) {
      conditions.add(cb.equal(keyIdExpr, queryBase.root.get(Key_.id)))
    }
    subquery.select(if (isCountQuery) keyIdExpr else cb.literal(1L))
    subquery.where(*conditions.toTypedArray())
    return wrapExistsOrIn(subquery)
  }

  /**
   * Wraps a subquery selecting `key_id` as either a correlated `EXISTS` (data query, with
   * LIMIT the planner short-circuits after finding enough matches) or a non-correlated
   * `k.id IN (…)` (count query, one scan + hash join across all keys).
   */
  private fun wrapExistsOrIn(subquery: Subquery<Long>): Predicate =
    if (isCountQuery) queryBase.root.get(Key_.id).`in`(subquery) else cb.exists(subquery)

  private fun languagePredicate(
    langIdExpr: Path<Long>,
    languageIds: List<Long>,
  ): Predicate =
    if (languageIds.size == 1) {
      cb.equal(langIdExpr, cb.literal(languageIds.first()))
    } else {
      langIdExpr.`in`(languageIds)
    }

  // ─── entity-specific subquery helpers ───────────────────────────────────

  private fun buildTranslationExists(
    languageIds: List<Long>,
    extra: (Root<Translation>) -> Predicate = { cb.conjunction() },
  ): Predicate =
    buildExistsOrIn(
      Translation::class.java,
      keyIdInSub = { it.get(Translation_.key).get(Key_.id) },
      languageIdInSub = { it.get(Translation_.language).get(Language_.id) },
      languageIds = languageIds,
      extraConditions = { listOf(extra(it)) },
    )

  private fun buildCommentExists(
    languageIds: List<Long>,
    unresolvedOnly: Boolean,
  ): Predicate =
    buildExistsOrIn(
      TranslationComment::class.java,
      keyIdInSub = { it.get(TranslationComment_.translation).get(Translation_.key).get(Key_.id) },
      languageIdInSub = {
        it.get(TranslationComment_.translation).get(Translation_.language).get(Language_.id)
      },
      languageIds = languageIds,
      extraConditions = { tc ->
        if (unresolvedOnly) {
          listOf(cb.equal(tc.get(TranslationComment_.state), TranslationCommentState.NEEDS_RESOLUTION))
        } else {
          emptyList()
        }
      },
    )

  private fun buildActiveSuggestionExists(languageIds: List<Long>): Predicate =
    buildExistsOrIn(
      TranslationSuggestion::class.java,
      keyIdInSub = { it.get(TranslationSuggestion_.key).get(Key_.id) },
      languageIdInSub = { it.get(TranslationSuggestion_.language).get(Language_.id) },
      languageIds = languageIds,
      extraConditions = { ts ->
        listOf(
          cb.equal(
            ts.get(TranslationSuggestion_.state),
            TranslationSuggestionState.ACTIVE,
          ),
        )
      },
    )

  private fun buildQaIssueExists(
    languageIds: List<Long>,
    checkTypes: List<QaCheckType>?,
  ): Predicate =
    buildExistsOrIn(
      TranslationQaIssue::class.java,
      keyIdInSub = { it.get(TranslationQaIssue_.translation).get(Translation_.key).get(Key_.id) },
      languageIdInSub = {
        it.get(TranslationQaIssue_.translation).get(Translation_.language).get(Language_.id)
      },
      languageIds = languageIds,
      extraConditions = { qa ->
        val base = cb.equal(qa.get(TranslationQaIssue_.state), QaIssueState.OPEN)
        if (!checkTypes.isNullOrEmpty()) {
          listOf(base, qa.get(TranslationQaIssue_.type).`in`(checkTypes))
        } else {
          listOf(base)
        }
      },
    )

  /**
   * Label filter — per-language because different label-id sets may be requested per
   * language. Uses a join on `Translation.labels`, so doesn't fit the generic helper.
   */
  private fun buildLabelExists(
    language: LanguageDto,
    labelIds: List<Long>,
  ): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val tRoot = subquery.from(Translation::class.java)
    val labelJoin = tRoot.join(Translation_.labels)
    val conditions =
      mutableListOf<Predicate>(
        cb.equal(tRoot.get(Translation_.language).get(Language_.id), cb.literal(language.id)),
        if (labelIds.size <= LABEL_IN_CLAUSE_CHUNK_SIZE) {
          labelJoin.get(Label_.id).`in`(labelIds)
        } else {
          cb.or(
            *labelIds
              .chunked(LABEL_IN_CLAUSE_CHUNK_SIZE)
              .map { chunk ->
                labelJoin.get(Label_.id).`in`(chunk)
              }.toTypedArray(),
          )
        },
      )
    if (!isCountQuery) {
      conditions.add(cb.equal(tRoot.get(Translation_.key).get(Key_.id), queryBase.root.get(Key_.id)))
    }
    subquery.select(if (isCountQuery) tRoot.get(Translation_.key).get(Key_.id) else cb.literal(1L))
    subquery.where(*conditions.toTypedArray())
    return wrapExistsOrIn(subquery)
  }

  // ─── parsed-filter caches ───────────────────────────────────────────────

  private fun languageIdsForTags(tags: List<String>?): List<Long>? {
    if (tags.isNullOrEmpty()) return null
    val ids = tags.mapNotNull { tag -> queryBase.languages.find { it.tag == tag }?.id }
    return ids.takeIf { it.isNotEmpty() }
  }

  private val filterByStateMap: Map<String, List<TranslationState>>? by lazy {
    params.filterState
      ?.let { filterStateStrings -> TranslationFilterByState.parseList(filterStateStrings) }
      ?.let { filterByState ->
        val map = mutableMapOf<String, MutableList<TranslationState>>()
        filterByState.forEach {
          map.getOrPut(it.languageTag) { mutableListOf() }.add(it.state)
        }
        return@lazy map
      }
  }

  private val filterByLabelMap: Map<String, List<TranslationFilterByLabel>>? by lazy {
    params.filterLabel?.let { filterLabels ->
      TranslationFilterByLabel
        .parseList(filterLabels)
        .let { filterLabelsParsed ->
          val map = mutableMapOf<String, MutableList<TranslationFilterByLabel>>()
          filterLabelsParsed.forEach {
            map.getOrPut(it.languageTag) { mutableListOf() }.add(it)
          }
          return@lazy map
        }
    }
  }
}
