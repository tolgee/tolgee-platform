package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.criteria.AbstractQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

/** Null means the feature is off and the AI review filters are ignored. */
data class AiReviewFilterData(
  val rules: List<AiReviewRule>,
  val branchingEnabled: Boolean,
  val defaultBranchName: String,
  val baseLanguageId: Long?,
)

/** Must match RuleScopeMatcher, or the "Not reviewed yet" filter disagrees with the rules. */
class AiReviewRuleSqlPredicates(
  private val cb: CriteriaBuilder,
  private val subquery: AbstractQuery<*>,
  private val data: AiReviewFilterData,
) {
  companion object {
    private const val WITHOUT_TAGS = ""
    private val ESCAPE_CHAR = WildcardLikeUtil.ESCAPE_CHAR

    fun coversLanguage(
      rule: AiReviewRule,
      languageId: Long,
    ): Boolean = rule.languages.isEmpty() || rule.languages.any { it.id == languageId }

    fun effectiveNever(
      rule: AiReviewRule,
      languageId: Long,
    ): Boolean = rule.overrides.find { it.language.id == languageId }?.never ?: rule.never
  }

  fun scopePredicate(
    rule: AiReviewRule,
    translation: Root<Translation>,
  ): Predicate {
    val key = translation.get(Translation_.key)
    val conditions = mutableListOf<Predicate>()

    if (rule.tagIn.isNotEmpty()) {
      conditions.add(anyTagMatches(rule.tagIn, key))
    }
    if (rule.tagNotIn.isNotEmpty()) {
      conditions.add(cb.not(anyTagMatches(rule.tagNotIn, key)))
    }
    if (rule.namespaceIn.isNotEmpty()) {
      conditions.add(namespaceMatches(rule.namespaceIn, key))
    }
    if (rule.namespaceNotIn.isNotEmpty()) {
      conditions.add(cb.not(namespaceMatches(rule.namespaceNotIn, key)))
    }
    if (data.branchingEnabled) {
      if (rule.branchIn.isNotEmpty()) {
        conditions.add(anyBranchMatches(rule.branchIn, key))
      }
      if (rule.branchNotIn.isNotEmpty()) {
        conditions.add(cb.not(anyBranchMatches(rule.branchNotIn, key)))
      }
    }
    if (conditions.isEmpty()) return cb.conjunction()
    return cb.and(*conditions.toTypedArray())
  }

  private fun anyTagMatches(
    ruleTags: Collection<String>,
    key: Path<Key>,
  ): Predicate {
    val parts = mutableListOf<Predicate>()
    val named = ruleTags.filter { it != WITHOUT_TAGS }
    if (named.isNotEmpty()) {
      val sub = subquery.subquery(Long::class.java)
      val keyMeta = sub.from(KeyMeta::class.java)
      val tag = keyMeta.join<KeyMeta, io.tolgee.model.key.Tag>("tags")
      sub.select(cb.literal(1L))
      sub.where(
        cb.equal(keyMeta.get<Key>("key").get<Long>("id"), key.get(Key_.id)),
        tag.get<String>("name").`in`(named),
      )
      parts.add(cb.exists(sub))
    }
    if (WITHOUT_TAGS in ruleTags) {
      val sub = subquery.subquery(Long::class.java)
      val keyMeta = sub.from(KeyMeta::class.java)
      keyMeta.join<KeyMeta, io.tolgee.model.key.Tag>("tags")
      sub.select(cb.literal(1L))
      sub.where(cb.equal(keyMeta.get<Key>("key").get<Long>("id"), key.get(Key_.id)))
      parts.add(cb.not(cb.exists(sub)))
    }
    return cb.or(*parts.toTypedArray())
  }

  private fun namespaceMatches(
    names: Collection<String>,
    key: Path<Key>,
  ): Predicate {
    val parts = mutableListOf<Predicate>()
    val named = names.filter { it != "" }
    if (named.isNotEmpty()) {
      val sub = subquery.subquery(Long::class.java)
      val namespace = sub.from(Namespace::class.java)
      sub.select(cb.literal(1L))
      sub.where(
        cb.equal(namespace, key.get(Key_.namespace)),
        namespace.get<String>("name").`in`(named),
      )
      parts.add(cb.exists(sub))
    }
    if ("" in names) {
      parts.add(cb.isNull(key.get(Key_.namespace)))
    }
    return cb.or(*parts.toTypedArray())
  }

  private fun anyBranchMatches(
    patterns: Collection<String>,
    key: Path<Key>,
  ): Predicate {
    val parts =
      patterns.map { pattern ->
        val like = WildcardLikeUtil.toLikePattern(WildcardLikeUtil.EXACT_PREFIX + pattern)
        val sub = subquery.subquery(Long::class.java)
        val branch = sub.from(io.tolgee.model.branching.Branch::class.java)
        sub.select(cb.literal(1L))
        sub.where(
          cb.equal(branch, key.get(Key_.branch)),
          cb.like(branch.get("name"), like, ESCAPE_CHAR),
        )
        val onNamedBranch = cb.exists(sub)
        val onDefaultBranch =
          cb.and(
            cb.isNull(key.get(Key_.branch)),
            cb.like(cb.literal(data.defaultBranchName), like, ESCAPE_CHAR),
          )
        cb.or(onNamedBranch, onDefaultBranch)
      }
    return cb.or(*parts.toTypedArray())
  }
}
