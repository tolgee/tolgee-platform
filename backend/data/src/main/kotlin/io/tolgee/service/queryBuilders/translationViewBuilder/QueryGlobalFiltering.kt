package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.Language_
import io.tolgee.model.UserAccount_
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityDescribingEntity_
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityModifiedEntity_
import io.tolgee.model.activity.ActivityRevision_
import io.tolgee.model.branching.Branch_
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.Tag_
import io.tolgee.model.task.TaskKey
import io.tolgee.model.task.TaskKey_
import io.tolgee.model.task.Task_
import io.tolgee.model.temp.UnsuccessfulJobKey
import io.tolgee.model.temp.UnsuccessfulJobKey_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Subquery
import org.hibernate.query.criteria.JpaCteContainer
import org.hibernate.query.criteria.JpaCteCriteria
import org.hibernate.query.sqm.tree.select.AbstractSqmSelectQuery
import org.hibernate.sql.ast.tree.cte.CteMaterialization
import java.util.Locale

class QueryGlobalFiltering(
  private val params: TranslationFilters,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val entityManager: EntityManager,
  private val isCountQuery: Boolean = false,
) {
  fun apply() {
    filterTag()
    filterNoTag()
    filterNamespace()
    filterNoNamespace()
    filterKeyName()
    filterKeyId()
    filterUntranslatedAny()
    filterTranslatedAny()
    filterHasScreenshot()
    filterHasNoScreenshot()
    filterSearch()
    filterRevisionId()
    filterFailedTargets()
    filterTask()
    filterBranch()
    filterDeletedByUserId()
  }

  private fun filterFailedTargets() {
    if (params.filterFailedKeysOfJob != null) {
      val subquery: Subquery<Long> = queryBase.query.subquery(Long::class.java)
      val unsuccessfulJobKey = subquery.from(UnsuccessfulJobKey::class.java)
      subquery.select(unsuccessfulJobKey.get(UnsuccessfulJobKey_.keyId))
      queryBase.whereConditions.add(
        queryBase.keyIdExpression.`in`(subquery),
      )
    }
  }

  private fun filterSearch() {
    val search = params.search
    if (search.isNullOrEmpty()) return
    val pattern = "%" + search.uppercase(Locale.getDefault()) + "%"

    // `fullTextFields` holds only key-level columns (key name, namespace name, description).
    // Translation text is searched via a non-correlated IN subquery so PostgreSQL scans the
    // translation table once and semi-joins with the outer key set — much faster than a
    // correlated EXISTS which would evaluate per outer key.
    val keyLevelRestrictions: MutableList<Predicate> =
      queryBase.fullTextFields
        .map { cb.like(cb.upper(it), pattern) }
        .toMutableList()

    val selectedLangIds = queryBase.languages.map { it.id }
    if (selectedLangIds.isNotEmpty()) {
      val subquery = queryBase.query.subquery(Long::class.java)
      val tRoot = subquery.from(Translation::class.java)
      subquery.select(tRoot.get(Translation_.key).get(Key_.id))
      subquery.where(
        cb.and(
          tRoot.get(Translation_.language).get(Language_.id).`in`(selectedLangIds),
          cb.like(cb.upper(tRoot.get(Translation_.text)), pattern),
        ),
      )
      keyLevelRestrictions.add(queryBase.root.get(Key_.id).`in`(subquery))
    }

    queryBase.whereConditions.add(cb.or(*keyLevelRestrictions.toTypedArray()))
  }

  private fun filterHasNoScreenshot() {
    if (params.filterHasNoScreenshot == true) {
      queryBase.whereConditions.add(cb.lt(queryBase.screenshotCountExpression, 1))
    }
  }

  private fun filterHasScreenshot() {
    if (params.filterHasScreenshot == true) {
      queryBase.whereConditions.add(cb.gt(queryBase.screenshotCountExpression, 0))
    }
  }

  private fun filterTranslatedAny() {
    if (params.filterTranslatedAny != true) return
    val selectedLangIds = queryBase.languages.map { it.id }
    if (selectedLangIds.isEmpty()) {
      queryBase.whereConditions.add(cb.disjunction())
      return
    }
    val subquery = queryBase.query.subquery(Long::class.java)
    val tRoot = subquery.from(Translation::class.java)
    val conditions = mutableListOf<Predicate>()
    if (isCountQuery) {
      subquery.select(tRoot.get(Translation_.key).get(Key_.id))
    } else {
      subquery.select(cb.literal(1L))
      conditions.add(cb.equal(tRoot.get(Translation_.key).get(Key_.id), queryBase.root.get(Key_.id)))
    }
    conditions.add(tRoot.get(Translation_.language).get(Language_.id).`in`(selectedLangIds))
    conditions.add(cb.isNotNull(tRoot.get(Translation_.text)))
    conditions.add(cb.notEqual(tRoot.get(Translation_.text), ""))
    subquery.where(*conditions.toTypedArray())
    queryBase.whereConditions.add(
      if (isCountQuery) queryBase.root.get(Key_.id).`in`(subquery) else cb.exists(subquery),
    )
  }

  private fun filterUntranslatedAny() {
    if (params.filterUntranslatedAny != true) return
    val selectedLangIds = queryBase.languages.map { it.id }
    if (selectedLangIds.isEmpty()) return

    // Materialized CTE: PostgreSQL computes the set of fully-translated key IDs once and
    // hashes it, then anti-joins against the outer key set. This is much faster than an
    // inline subquery which the planner may evaluate differently per outer row.
    val cte = buildFullyTranslatedKeysCte(selectedLangIds)

    val subquery = queryBase.query.subquery(Long::class.java)
    val cteRoot = (subquery as AbstractSqmSelectQuery<Long>).from(cte)
    subquery.select(cteRoot.get<Long>("keyId"))
    queryBase.whereConditions.add(cb.not(queryBase.root.get(Key_.id).`in`(subquery)))
  }

  /**
   * Builds a materialized CTE that selects key IDs having a non-empty translation in ALL
   * selected languages. Registered on the outer [queryBase.query].
   */
  private fun buildFullyTranslatedKeysCte(selectedLangIds: List<Long>): JpaCteCriteria<Tuple> {
    val cteQuery = cb.createTupleQuery()
    val tRoot = cteQuery.from(Translation::class.java)
    val keyIdPath = tRoot.get(Translation_.key).get(Key_.id)
    cteQuery.multiselect(keyIdPath.alias("keyId"))
    cteQuery.where(
      cb.and(
        tRoot.get(Translation_.language).get(Language_.id).`in`(selectedLangIds),
        cb.isNotNull(tRoot.get(Translation_.text)),
        cb.notEqual(tRoot.get(Translation_.text), ""),
      ),
    )
    cteQuery.groupBy(keyIdPath)
    cteQuery.having(cb.equal(cb.count(tRoot), cb.literal(selectedLangIds.size.toLong())))

    val cte = (queryBase.query as JpaCteContainer).with(cteQuery)
    cte.setMaterialization(CteMaterialization.MATERIALIZED)
    return cte
  }

  private fun filterKeyId() {
    if (params.filterKeyId != null) {
      queryBase.whereConditions.add(queryBase.keyIdExpression.`in`(params.filterKeyId))
    }
  }

  private fun filterKeyName() {
    if (params.filterKeyName != null) {
      queryBase.whereConditions.add(queryBase.keyNameExpression.`in`(params.filterKeyName))
    }
  }

  private fun filterNamespace() {
    val filterNamespace = distinguishEmptyValue(params.filterNamespace)
    if (filterNamespace != null) {
      val inCondition = queryBase.namespaceNameExpression.`in`(filterNamespace)
      val hasDefaultNamespace = filterNamespace.contains("")
      val condition =
        if (hasDefaultNamespace) {
          cb.or(inCondition, queryBase.namespaceNameExpression.isNull)
        } else {
          inCondition
        }
      queryBase.whereConditions.add(condition)
    }
  }

  private fun filterNoNamespace() {
    val filterNoNamespace = distinguishEmptyValue(params.filterNoNamespace)
    if (filterNoNamespace != null) {
      val query = queryBase.query
      val root = queryBase.root

      val subquery = query.subquery(Long::class.java)
      val subRoot = subquery.from(Key::class.java)
      val subJoin = subRoot.join(Key_.namespace)
      val subquerySelect = subquery.select(subRoot.get(Key_.id))
      val hasDefaultNamespace = filterNoNamespace.contains("")

      if (isCountQuery) {
        subquerySelect.where(
          subJoin.get(Namespace_.name).`in`(filterNoNamespace),
        )
        queryBase.whereConditions.add(cb.not(root.get(Key_.id).`in`(subquery)))
      } else {
        subquerySelect.where(
          cb.equal(subRoot.get(Key_.id), root.get(Key_.id)),
          subJoin.get(Namespace_.name).`in`(filterNoNamespace),
        )
        queryBase.whereConditions.add(cb.not(cb.exists(subquery)))
      }
      if (hasDefaultNamespace) {
        queryBase.whereConditions.add(
          queryBase.namespaceNameExpression.isNotNull,
        )
      }
    }
  }

  private fun distinguishEmptyValue(list: List<String>?): List<String>? {
    if (list != null && list.isEmpty()) {
      return listOf("")
    }
    return list
  }

  private fun filterTag() {
    val filterTag = distinguishEmptyValue(params.filterTag) ?: return
    val nonEmptyTags = filterTag.filter { it.isNotEmpty() }
    val hasEmptyTag = filterTag.contains("")

    val predicates = mutableListOf<Predicate>()
    if (nonEmptyTags.isNotEmpty()) {
      predicates.add(tagSubqueryPredicate(nonEmptyTags, negate = false))
    }
    if (hasEmptyTag) {
      // empty string in filterTag = "keys with no tags at all"
      predicates.add(hasAnyTagPredicate(negate = true))
    }
    if (predicates.isNotEmpty()) {
      queryBase.whereConditions.add(cb.or(*predicates.toTypedArray()))
    }
  }

  private fun filterNoTag() {
    val filterNoTag = distinguishEmptyValue(params.filterNoTag) ?: return
    val nonEmptyTags = filterNoTag.filter { it.isNotEmpty() }
    val hasEmptyTag = filterNoTag.contains("")

    if (nonEmptyTags.isNotEmpty()) {
      queryBase.whereConditions.add(tagSubqueryPredicate(nonEmptyTags, negate = true))
    }
    if (hasEmptyTag) {
      // empty string in filterNoTag = "not without tag" = key must have at least one tag
      queryBase.whereConditions.add(hasAnyTagPredicate(negate = false))
    }
  }

  /**
   * Builds an EXISTS/IN predicate matching keys tagged with any of [tagNames].
   * When [negate] is true, returns the negated form (keys NOT tagged with any of them).
   */
  private fun tagSubqueryPredicate(
    tagNames: List<String>,
    negate: Boolean,
  ): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val kmRoot = subquery.from(KeyMeta::class.java)
    val tagJoin = kmRoot.join(KeyMeta_.tags)
    val predicate = buildTagSubquery(subquery, kmRoot, tagJoin.get(Tag_.name).`in`(tagNames))
    return if (negate) cb.not(predicate) else predicate
  }

  /**
   * Builds an EXISTS/IN predicate matching keys that have at least one tag.
   * When [negate] is true, matches keys with NO tags.
   */
  private fun hasAnyTagPredicate(negate: Boolean): Predicate {
    val subquery = queryBase.query.subquery(Long::class.java)
    val kmRoot = subquery.from(KeyMeta::class.java)
    val tagJoin = kmRoot.join(KeyMeta_.tags)
    val predicate = buildTagSubquery(subquery, kmRoot, cb.isNotNull(tagJoin.get(Tag_.id)))
    return if (negate) cb.not(predicate) else predicate
  }

  /**
   * Shared helper: configures a tag subquery as either correlated EXISTS (data query) or
   * non-correlated IN (count query), and returns the appropriate predicate.
   */
  private fun buildTagSubquery(
    subquery: Subquery<Long>,
    kmRoot: jakarta.persistence.criteria.Root<KeyMeta>,
    condition: Predicate,
  ): Predicate {
    val keyIdPath = kmRoot.get(KeyMeta_.key).get(Key_.id)
    if (isCountQuery) {
      subquery.select(keyIdPath)
      subquery.where(condition)
      return queryBase.root.get(Key_.id).`in`(subquery)
    }
    subquery.select(cb.literal(1L))
    subquery.where(cb.and(cb.equal(keyIdPath, queryBase.root.get(Key_.id)), condition))
    return cb.exists(subquery)
  }

  private fun filterTask() {
    val taskNumbers = params.filterTaskNumber ?: return

    val subquery = queryBase.query.subquery(Long::class.java)
    val tkRoot = subquery.from(TaskKey::class.java)
    val conditions = mutableListOf<Predicate>()
    if (isCountQuery) {
      subquery.select(tkRoot.get(TaskKey_.key).get(Key_.id))
    } else {
      subquery.select(cb.literal(1L))
      conditions.add(cb.equal(tkRoot.get(TaskKey_.key).get(Key_.id), queryBase.root.get(Key_.id)))
    }
    conditions.add(tkRoot.get(TaskKey_.task).get(Task_.number).`in`(taskNumbers))
    if (params.filterTaskKeysNotDone == true) {
      conditions.add(cb.equal(tkRoot.get(TaskKey_.done), false))
    }
    if (params.filterTaskKeysDone == true) {
      conditions.add(cb.equal(tkRoot.get(TaskKey_.done), true))
    }
    subquery.where(cb.and(*conditions.toTypedArray()))
    queryBase.whereConditions.add(
      if (isCountQuery) queryBase.root.get(Key_.id).`in`(subquery) else cb.exists(subquery),
    )
  }

  private fun filterRevisionId() {
    if (!params.filterRevisionId.isNullOrEmpty()) {
      val modifiedEntitySubquery = queryBase.query.subquery(Long::class.java)
      val meRoot = modifiedEntitySubquery.from(ActivityModifiedEntity::class.java)
      modifiedEntitySubquery.select(meRoot.get(ActivityModifiedEntity_.entityId))
      modifiedEntitySubquery.where(
        cb.and(
          cb.equal(meRoot.get(ActivityModifiedEntity_.entityClass), "Key"),
          meRoot.get(ActivityModifiedEntity_.activityRevision).get(ActivityRevision_.id).`in`(params.filterRevisionId),
        ),
      )

      val describingEntitySubquery = queryBase.query.subquery(Long::class.java)
      val deRoot = describingEntitySubquery.from(ActivityDescribingEntity::class.java)
      describingEntitySubquery.select(deRoot.get(ActivityDescribingEntity_.entityId))
      describingEntitySubquery.where(
        cb.and(
          cb.equal(deRoot.get(ActivityDescribingEntity_.entityClass), "Key"),
          deRoot
            .get(
              ActivityDescribingEntity_.activityRevision,
            ).get(ActivityRevision_.id)
            .`in`(params.filterRevisionId),
        ),
      )

      queryBase.whereConditions.add(
        cb.or(
          queryBase.root.get(Key_.id).`in`(modifiedEntitySubquery),
          queryBase.root.get(Key_.id).`in`(describingEntitySubquery),
        ),
      )
    }
  }

  private fun filterBranch() {
    val branchJoin =
      queryBase.branchJoin
        ?: queryBase.root.join(Key_.branch, JoinType.LEFT).also { queryBase.branchJoin = it }
    if (params.filterKeyId?.isNotEmpty() == true) {
      return
    }
    if (params.branch.isNullOrEmpty()) {
      queryBase.whereConditions.add(
        cb.or(
          branchJoin.get(Branch_.id).isNull,
          cb.isTrue(branchJoin.get(Branch_.isDefault)),
        ),
      )
    } else {
      queryBase.whereConditions.add(
        cb.and(
          cb.equal(branchJoin.get(Branch_.name), cb.literal(params.branch)),
          cb.isNull(branchJoin.get(Branch_.deletedAt)),
        ),
      )
    }
  }

  private fun filterDeletedByUserId() {
    if (!params.filterDeletedByUserId.isNullOrEmpty()) {
      val deletedByJoin =
        queryBase.deletedByJoin
          ?: queryBase.root.join(Key_.deletedBy, JoinType.LEFT).also { queryBase.deletedByJoin = it }
      queryBase.whereConditions.add(
        deletedByJoin.get(UserAccount_.id).`in`(params.filterDeletedByUserId),
      )
    }
  }
}
