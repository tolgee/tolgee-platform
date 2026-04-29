package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.Language_
import io.tolgee.model.Project_
import io.tolgee.model.Screenshot
import io.tolgee.model.Screenshot_
import io.tolgee.model.UserAccount
import io.tolgee.model.UserAccount_
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.Branch_
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference_
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.model.keyBigMeta.KeysDistance_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import java.util.Date

/**
 * Foundation for the Translation View query.
 *
 * This query returns **only key-level data**. It does not join the translations table at all —
 * instead:
 *
 * - Translation-level filters are implemented as `EXISTS` subqueries on `Translation` via
 *   [QueryTranslationFiltering].
 * - Sorting and cursor pagination by `translations.{tag}.text` use scalar correlated subqueries
 *   built by [scalarTranslationText].
 * - Full-text search across translation texts is a single `EXISTS` subquery spanning all selected
 *   languages (see [QueryGlobalFiltering.filterSearch]).
 * - The actual translation data is fetched in a **separate query** by
 *   [TranslationViewDataProvider] using `WHERE key.id IN (...) AND language.id IN (...)` and
 *   assembled into the returned views in application code.
 *
 * The motivation for this structure is performance: a previous design LEFT-JOINed the
 * `translation` table once per requested language, which grew linearly with language count and
 * dominated cold-cache query time. With the filters expressed as EXISTS subqueries, the main
 * query touches a constant number of tables regardless of how many languages are requested.
 */
class QueryBase<T>(
  private val cb: CriteriaBuilder,
  private val projectId: Long,
  val query: CriteriaQuery<T>,
  val languages: Set<LanguageDto>,
  private val params: TranslationFilters,
  private val entityManager: EntityManager,
  private val qaEnabled: Boolean,
  val isCountQuery: Boolean = false,
) {
  val whereConditions: MutableSet<Predicate> = HashSet()

  /**
   * Translation-level filter predicates (one per active per-language filter). They are OR-ed
   * together at query build time in [TranslationsViewQueryBuilder.getWhereConditions]. Filters
   * across different languages and filter types are thus combined with OR semantics — e.g.
   * `filterState=en,TRANSLATED` together with `filterHasUnresolvedCommentsInLang=de` returns the
   * union of keys matching either condition, not the intersection.
   */
  val translationConditions: MutableSet<Predicate> = HashSet()
  val root: Root<Key> = query.from(Key::class.java)
  val keyNameExpression: Path<String> = root.get(Key_.name)
  val keyCreatedAtExpression: Path<Date> = root.get(Key_.createdAt)
  val keyIsPluralExpression: Path<Boolean> = root.get(Key_.isPlural)
  val keyArgNameExpression: Path<String?> = root.get(Key_.pluralArgName)
  val keyMaxCharLimitExpression: Path<Int?> = root.get(Key_.maxCharLimit)
  val keyIdExpression: Path<Long> = root.get(Key_.id)
  val querySelection = QuerySelection()
  val fullTextFields: MutableSet<Expression<String>> = HashSet()
  lateinit var namespaceNameExpression: Path<String>
  lateinit var screenshotCountExpression: Expression<Long>
  var branchJoin: Join<Key, Branch>? = null
  var deletedByJoin: Join<Key, UserAccount>? = null
  private val queryGlobalFiltering = QueryGlobalFiltering(params, this, cb, entityManager, isCountQuery)
  var queryTranslationFiltering = QueryTranslationFiltering(params, this, cb, isCountQuery)

  init {
    querySelection[KeyWithTranslationsView::keyId.name] = keyIdExpression
    querySelection[KeyWithTranslationsView::createdAt.name] = keyCreatedAtExpression
    querySelection[KeyWithTranslationsView::keyName.name] = keyNameExpression
    querySelection[KeyWithTranslationsView::keyIsPlural.name] = keyIsPluralExpression
    querySelection[KeyWithTranslationsView::keyPluralArgName.name] = keyArgNameExpression
    querySelection[KeyWithTranslationsView::keyMaxCharLimit.name] = keyMaxCharLimitExpression
    whereConditions.add(cb.equal(root.get<Any>(Key_.PROJECT).get<Any>(Project_.ID), this.projectId))
    if (params.trashed) {
      whereConditions.add(cb.isNotNull(root.get(Key_.deletedAt)))
    } else {
      whereConditions.add(cb.isNull(root.get(Key_.deletedAt)))
    }
    fullTextFields.add(root.get(Key_.name))
    addLeftJoinedColumns()
    applyTranslationFilters()
    queryGlobalFiltering.apply()
  }

  private fun addLeftJoinedColumns() {
    addBranch()
    addNamespace()
    addDescription()
    addScreenshotCounts()
    addContextCounts()
    if (params.trashed) {
      addDeletedAtSelection()
      addDeletedBySelection()
    }
  }

  /**
   * Invokes every translation-level filter in [QueryTranslationFiltering]. Each filter method
   * is called **once** (not per language) and internally collapses its multi-language input
   * into a single `language_id IN (…)` subquery wherever the semantics allow — see the
   * "multi-language collapse" note in [QueryTranslationFiltering].
   *
   * All resulting predicates are added to [translationConditions] and later OR-ed together in
   * [TranslationsViewQueryBuilder.getWhereConditions].
   *
   * QA-related filters (`filterHasQaIssuesInLang`, `filterQaCheckType`) are only applied when
   * `qaEnabled` is true; on projects without the QA Checks feature the filter parameters are
   * silently ignored.
   */
  private fun applyTranslationFilters() {
    queryTranslationFiltering.applyStateFilters()
    queryTranslationFiltering.applyAutoTranslatedFilter()
    queryTranslationFiltering.applyUntranslatedInLangFilter()
    queryTranslationFiltering.applyTranslatedInLangFilter()
    queryTranslationFiltering.applyHasCommentsFilter()
    queryTranslationFiltering.applyHasUnresolvedCommentsFilter()
    queryTranslationFiltering.applyHasSuggestionsFilter()
    queryTranslationFiltering.applyHasNoSuggestionsFilter()
    queryTranslationFiltering.applyLabelFilter()
    if (qaEnabled) {
      queryTranslationFiltering.applyQaFilters()
    }
    queryTranslationFiltering.applyOutdatedFilters()
  }

  /**
   * Builds a scalar correlated subquery that yields the `Translation.text` of the given language
   * for the current key row. Used for:
   *
   *  - `ORDER BY (subquery) ...` when sorting by `translations.{tag}.text`
   *  - `WHERE (subquery) > :cursor` when paginating over a translation-text sort column
   *
   * The `(key_id, language_id)` uniqueness constraint on the `translation` table guarantees at
   * most one row, so no `LIMIT 1` is needed. The subquery returns `NULL` when the key has no
   * translation in that language; ordering honors this via `NULLS FIRST` (asc) / `NULLS LAST`
   * (desc) set in [TranslationsViewQueryBuilder.getOrderList].
   */
  fun scalarTranslationText(languageId: Long): Expression<String> {
    val subquery = this.query.subquery(String::class.java)
    val subRoot = subquery.from(Translation::class.java)
    subquery.select(subRoot.get(Translation_.text))
    subquery.where(
      cb.and(
        cb.equal(subRoot.get(Translation_.key).get(Key_.id), this.root.get(Key_.id)),
        cb.equal(subRoot.get(Translation_.language).get(Language_.id), cb.literal(languageId)),
      ),
    )
    return subquery as Expression<String>
  }

  private fun addScreenshotCounts() {
    val screenshotSubquery = this.query.subquery(Long::class.java)
    val screenshotRoot = screenshotSubquery.from(Screenshot::class.java)
    val screenshotCount = cb.count(screenshotRoot.get(Screenshot_.id))
    screenshotSubquery.select(screenshotCount)
    screenshotSubquery.where(screenshotRoot.get(Screenshot_.id).`in`(getScreenshotIdFilterSubquery()))
    screenshotCountExpression = screenshotSubquery.selection
    this.querySelection[KeyWithTranslationsView::screenshotCount.name] = screenshotCountExpression
  }

  private fun getScreenshotIdFilterSubquery(): Subquery<Long> {
    val subquery = this.query.subquery(Long::class.java)
    val subQueryRoot = subquery.from(Key::class.java)
    val keyScreenshotReference = subQueryRoot.join(Key_.keyScreenshotReferences)
    subquery.where(cb.equal(subQueryRoot.get(Key_.id), this.root.get(Key_.id)))
    return subquery.select(keyScreenshotReference.get(KeyScreenshotReference_.screenshot).get(Screenshot_.id))
  }

  private fun addBranch() {
    val branch = this.root.join(Key_.branch, JoinType.LEFT)
    this.branchJoin = branch
    val branchName = branch.get(Branch_.name)
    this.querySelection[KeyWithTranslationsView::branch.name] = branchName
  }

  private fun addNamespace() {
    val namespace = this.root.join(Key_.namespace, JoinType.LEFT)
    val namespaceId = namespace.get(Namespace_.id)
    val namespaceName = namespace.get(Namespace_.name)
    namespaceNameExpression = namespaceName
    this.querySelection[KeyWithTranslationsView::keyNamespaceId.name] = namespaceId
    this.querySelection[KeyWithTranslationsView::keyNamespace.name] = namespaceName
    this.fullTextFields.add(namespaceName)
  }

  private fun addDescription() {
    val keyMeta = this.root.join(Key_.keyMeta, JoinType.LEFT)
    val description = keyMeta.get(KeyMeta_.description)
    this.querySelection[KeyWithTranslationsView::keyDescription.name] = description
    this.fullTextFields.add(description)
  }

  private fun addContextCounts() {
    val contextSubquery = this.query.subquery(Long::class.java)
    val contextRoot = contextSubquery.from(KeysDistance::class.java)
    contextSubquery.select(contextRoot.get(KeysDistance_.key1Id))
    contextSubquery.where(
      cb.or(
        cb.equal(this.root.get(Key_.id), contextRoot.get(KeysDistance_.key1Id)),
        cb.equal(this.root.get(Key_.id), contextRoot.get(KeysDistance_.key2Id)),
      ),
    )
    this.querySelection[KeyWithTranslationsView::contextPresent.name] = cb.exists(contextSubquery)
  }

  private fun addDeletedAtSelection() {
    querySelection[KeyWithTranslationsView::deletedAt.name] = root.get(Key_.deletedAt)
  }

  private fun addDeletedBySelection() {
    val deletedByJoin = root.join(Key_.deletedBy, JoinType.LEFT)
    this.deletedByJoin = deletedByJoin
    querySelection[KeyWithTranslationsView::deletedByUserId.name] = deletedByJoin.get(UserAccount_.id)
    querySelection[KeyWithTranslationsView::deletedByUserName.name] = deletedByJoin.get(UserAccount_.name)
    querySelection[KeyWithTranslationsView::deletedByUserUsername.name] = deletedByJoin.get(UserAccount_.username)
    querySelection[KeyWithTranslationsView::deletedByUserAvatarHash.name] = deletedByJoin.get(UserAccount_.avatarHash)
    querySelection[KeyWithTranslationsView::deletedByUserDeletedAt.name] = deletedByJoin.get(UserAccount_.deletedAt)
  }

  val Expression<String>.isNotNullOrBlank: Predicate
    get() = cb.and(cb.isNotNull(this), cb.notEqual(this, ""))

  val Expression<String>.isNullOrBlank: Predicate
    get() = cb.or(cb.isNull(this), cb.equal(this, ""))
}
