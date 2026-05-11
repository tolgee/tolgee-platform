package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language_
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import org.hibernate.query.criteria.JpaCteContainer
import org.hibernate.query.criteria.JpaCteCriteria
import org.hibernate.query.sqm.tree.select.AbstractSqmSelectQuery
import org.hibernate.sql.ast.tree.cte.CteMaterialization

/**
 * Builds the predicate for the `filterState` filter.
 *
 * `filterState` is the most complex translation-level filter in the system — enough that it
 * deserves its own class. It has three dispatch branches, each with different SQL shapes:
 *
 *  1. **Single language** → [singleLanguageStatePredicate]: one `EXISTS` or `NOT EXISTS`
 *     subquery, depending on whether the state set contains `UNTRANSLATED` (which requires
 *     matching missing rows as well).
 *  2. **Multiple languages, same state set, data query** → [homogeneousStateCountPredicate]:
 *     one correlated count-comparison per key, ~3× faster than N per-language subqueries.
 *  3. **Anything else (heterogeneous, or homogeneous count query)** →
 *     [collapsedMultiStatePredicate]: one combined positive subquery with OR-ed
 *     `(lang=L AND state IN S)` pairs, plus one missing-row `NOT EXISTS` / `NOT IN` per
 *     UNTRANSLATED-containing language.
 *
 * **UNTRANSLATED / missing-row equivalence**: a missing translation row is semantically the
 * same as `state = UNTRANSLATED` in this codebase's sparse-data model (main does the same;
 * see its `cb.or(condition, cb.isNull(translationStateField))` branch). The extra missing-row
 * predicate in branch 3 and the `NOT EXISTS` form in branch 1 both encode this.
 */
internal class StateFilterBuilder(
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val isCountQuery: Boolean,
) {
  /**
   * Returns the combined state-filter predicate to add to `translationConditions`, or `null`
   * when there is no effective state filter (unknown tags silently dropped).
   */
  fun build(perLanguage: List<Pair<LanguageDto, Set<TranslationState>>>): Predicate? {
    if (perLanguage.isEmpty()) return null

    if (perLanguage.size == 1) {
      val (language, states) = perLanguage.first()
      return singleLanguageStatePredicate(language, states)
    }

    val firstStates = perLanguage.first().second
    val allSameStates = perLanguage.all { it.second == firstStates }
    // Homogeneous case: all languages request the same state set. Uses a materialized CTE
    // (for UNTRANSLATED) or a single EXISTS (for other states) — both count and data queries.
    if (allSameStates) {
      return homogeneousStateCountPredicate(perLanguage.map { it.first }, firstStates)
    }

    return collapsedMultiStatePredicate(perLanguage)
  }

  /**
   * Matches keys where a given single language is in one of the requested states.
   *
   *  - UNTRANSLATED ∈ S → `NOT EXISTS` of a row with a state **outside** S. This matches both
   *    keys with no row in L and keys with a row in L whose state is in S.
   *  - UNTRANSLATED ∉ S → plain `EXISTS` of a row in L with a state in S.
   */
  private fun singleLanguageStatePredicate(
    language: LanguageDto,
    states: Set<TranslationState>,
  ): Predicate {
    return if (TranslationState.UNTRANSLATED in states) {
      val disallowed = TranslationState.entries.filter { it !in states }
      cb.not(
        buildTranslationExists(language) { t ->
          if (disallowed.isEmpty()) {
            cb.disjunction()
          } else {
            t.get(Translation_.state).`in`(disallowed)
          }
        },
      )
    } else {
      buildTranslationExists(language) { t -> t.get(Translation_.state).`in`(states) }
    }
  }

  /**
   * Optimized predicate for the homogeneous case (all languages request the same state set).
   *
   * For state sets **with UNTRANSLATED**: finds keys where ALL selected languages have a
   * disallowed state, then excludes them. A non-correlated GROUP BY + HAVING subquery builds
   * the "fully disallowed" key set once and semi-anti-joins it — much faster than the
   * previous correlated COUNT(*) per key.
   *
   * For state sets **without UNTRANSLATED**: a single correlated EXISTS checks if any row
   * matches the requested (lang, state) pair — no missing-row handling needed.
   */
  private fun homogeneousStateCountPredicate(
    languages: List<LanguageDto>,
    states: Set<TranslationState>,
  ): Predicate {
    val langIds = languages.map { it.id }
    return if (TranslationState.UNTRANSLATED in states) {
      val disallowed = TranslationState.entries.filter { it !in states }
      // All states requested (including UNTRANSLATED) → every key matches unconditionally.
      if (disallowed.isEmpty()) return cb.conjunction()

      // Materialized CTE: PostgreSQL computes the set of keys where every selected language
      // has a disallowed state once, then anti-joins against the outer key set.
      val cte = buildFullyDisallowedStateCte(langIds, disallowed)
      val subquery = queryBase.query.subquery(Long::class.java)
      val cteRoot = (subquery as AbstractSqmSelectQuery<Long>).from(cte)
      subquery.select(cteRoot.get<Long>("keyId"))
      cb.not(queryBase.root.get(Key_.id).`in`(subquery))
    } else {
      val subquery = queryBase.query.subquery(Long::class.java)
      val tRoot = subquery.from(Translation::class.java)
      val conditions = mutableListOf<Predicate>()
      if (isCountQuery) {
        subquery.select(tRoot.get(Translation_.key).get(Key_.id))
      } else {
        subquery.select(cb.literal(1L))
        conditions.add(cb.equal(tRoot.get(Translation_.key).get(Key_.id), queryBase.root.get(Key_.id)))
      }
      conditions.add(tRoot.get(Translation_.language).get(Language_.id).`in`(langIds))
      conditions.add(tRoot.get(Translation_.state).`in`(states))
      subquery.where(*conditions.toTypedArray())
      if (isCountQuery) queryBase.root.get(Key_.id).`in`(subquery) else cb.exists(subquery)
    }
  }

  /**
   * Heterogeneous (or homogeneous-count) form. Emits a single combined subquery with OR-ed
   * `(lang=L AND state IN S)` pairs, plus one missing-row `NOT EXISTS` / `NOT IN` per
   * UNTRANSLATED-containing language — all OR-ed together.
   */
  private fun collapsedMultiStatePredicate(perLanguage: List<Pair<LanguageDto, Set<TranslationState>>>): Predicate {
    val disjuncts = mutableListOf<Predicate>()

    val positiveTuples = perLanguage.filter { (_, states) -> states.isNotEmpty() }
    if (positiveTuples.isNotEmpty()) {
      disjuncts.add(buildCombinedStateExists(positiveTuples))
    }

    // "Missing in at least one lang" cannot be collapsed with IN — that would mean "missing
    // in all langs" (intersection instead of union). So one NOT-form per UNT-containing lang.
    val missingRowLanguages = perLanguage.filter { (_, states) -> TranslationState.UNTRANSLATED in states }
    for ((language, _) in missingRowLanguages) {
      disjuncts.add(cb.not(buildTranslationExists(language) { cb.conjunction() }))
    }

    return cb.or(*disjuncts.toTypedArray())
  }

  /**
   * The OR-of-pairs subquery at the heart of [collapsedMultiStatePredicate].
   */
  private fun buildCombinedStateExists(perLanguage: List<Pair<LanguageDto, Set<TranslationState>>>): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val tRoot = subquery.from(Translation::class.java)
    val perLangOrs =
      perLanguage.map { (language, states) ->
        cb.and(
          cb.equal(tRoot.get(Translation_.language).get(Language_.id), cb.literal(language.id)),
          tRoot.get(Translation_.state).`in`(states),
        )
      }
    val pairs = cb.or(*perLangOrs.toTypedArray())
    if (isCountQuery) {
      subquery.select(tRoot.get(Translation_.key).get(Key_.id))
      subquery.where(pairs)
      return queryBase.root.get(Key_.id).`in`(subquery)
    }
    subquery.select(cb.literal(1L))
    subquery.where(
      cb.and(
        cb.equal(tRoot.get(Translation_.key).get(Key_.id), queryBase.root.get(Key_.id)),
        pairs,
      ),
    )
    return cb.exists(subquery)
  }

  /**
   * EXISTS / `IN` subquery over the `Translation` table for a single language. Local copy of
   * the same helper in [QueryTranslationFiltering] — the state-filter subqueries have unique
   * shapes (multi-predicate OR, count aggregate) so they're kept self-contained here rather
   * than shared through a generic helper.
   */
  private fun buildTranslationExists(
    language: LanguageDto,
    extra: (jakarta.persistence.criteria.Root<Translation>) -> Predicate,
  ): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val tRoot = subquery.from(Translation::class.java)
    val langEq = cb.equal(tRoot.get(Translation_.language).get(Language_.id), cb.literal(language.id))
    val extraP = extra(tRoot)
    if (isCountQuery) {
      subquery.select(tRoot.get(Translation_.key).get(Key_.id))
      subquery.where(cb.and(langEq, extraP))
      return queryBase.root.get(Key_.id).`in`(subquery)
    }
    subquery.select(cb.literal(1L))
    subquery.where(
      cb.and(
        cb.equal(tRoot.get(Translation_.key).get(Key_.id), queryBase.root.get(Key_.id)),
        langEq,
        extraP,
      ),
    )
    return cb.exists(subquery)
  }

  /**
   * Builds a materialized CTE selecting key IDs where every selected language has a
   * disallowed state. Registered on the outer [queryBase.query].
   */
  private fun buildFullyDisallowedStateCte(
    langIds: List<Long>,
    disallowed: List<TranslationState>,
  ): JpaCteCriteria<Tuple> {
    val cteQuery = cb.createTupleQuery()
    val tRoot = cteQuery.from(Translation::class.java)
    val keyIdPath = tRoot.get(Translation_.key).get(Key_.id)
    cteQuery.multiselect(keyIdPath.alias("keyId"))
    cteQuery.where(
      cb.and(
        tRoot.get(Translation_.language).get(Language_.id).`in`(langIds),
        tRoot.get(Translation_.state).`in`(disallowed),
      ),
    )
    cteQuery.groupBy(keyIdPath)
    cteQuery.having(cb.equal(cb.count(tRoot), cb.literal(langIds.size.toLong())))

    val cte = (queryBase.query as JpaCteContainer).with("fullyDisallowedStateKeys", cteQuery)
    cte.setMaterialization(CteMaterialization.MATERIALIZED)
    return cte
  }
}
