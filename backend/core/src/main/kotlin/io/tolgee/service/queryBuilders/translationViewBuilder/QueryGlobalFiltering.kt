package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityDescribingEntity_
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityModifiedEntity_
import io.tolgee.model.activity.ActivityRevision_
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
import io.tolgee.model.key.Namespace_
import io.tolgee.model.key.Tag_
import io.tolgee.model.task.TaskKey_
import io.tolgee.model.task.Task_
import io.tolgee.model.temp.UnsuccessfulJobKey
import io.tolgee.model.temp.UnsuccessfulJobKey_
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Subquery
import java.util.Locale

class QueryGlobalFiltering(
  private val params: TranslationFilters,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val entityManager: EntityManager,
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
    if (!search.isNullOrEmpty()) {
      val fullTextRestrictions: MutableSet<Predicate> = HashSet()
      for (fullTextField in queryBase.fullTextFields) {
        fullTextRestrictions.add(
          cb.like(
            cb.upper(fullTextField),
            "%" + search.uppercase(Locale.getDefault()) + "%",
          ),
        )
      }
      queryBase.whereConditions.add(cb.or(*fullTextRestrictions.toTypedArray()))
    }
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
    if (params.filterTranslatedAny == true) {
      val predicates =
        queryBase.translationsTextFields
          .map { with(queryBase) { it.isNotNullOrBlank } }
          .toTypedArray()
      queryBase.whereConditions.add(cb.or(*predicates))
    }
  }

  private fun filterUntranslatedAny() {
    if (params.filterUntranslatedAny == true) {
      val predicates =
        queryBase.translationsTextFields
          .map { with(queryBase) { it.isNullOrBlank } }
          .toTypedArray()
      queryBase.whereConditions.add(cb.or(*predicates))
    }
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

      subquerySelect.where(
        cb.equal(subRoot.get(Key_.id), root.get(Key_.id)),
        subJoin.get(Namespace_.name).`in`(filterNoNamespace),
      )
      queryBase.whereConditions.add(cb.not(cb.exists(subquery)))
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
    val filterTag = distinguishEmptyValue(params.filterTag)
    if (filterTag != null) {
      val keyMetaJoin = queryBase.root.join(Key_.keyMeta, JoinType.LEFT)
      val tagsJoin = keyMetaJoin.join(KeyMeta_.tags, JoinType.LEFT)
      val hasEmptyTag = filterTag.contains("")
      val inCondition = tagsJoin.get(Tag_.name).`in`(filterTag)
      val condition =
        if (hasEmptyTag) {
          cb.or(inCondition, tagsJoin.get(Tag_.name).isNull)
        } else {
          inCondition
        }
      queryBase.whereConditions.add(condition)
    }
  }

  private fun filterNoTag() {
    val filterNoTag = distinguishEmptyValue(params.filterNoTag)
    if (filterNoTag != null) {
      val query = queryBase.query
      val root = queryBase.root
      val keyMetaJoin = queryBase.root.join(Key_.keyMeta, JoinType.LEFT)
      val tagsJoin = keyMetaJoin.join(KeyMeta_.tags, JoinType.LEFT)

      val subquery = query.subquery(Long::class.java)
      val subRoot = subquery.from(Key::class.java)
      val subJoin = subRoot.join(Key_.keyMeta).join(KeyMeta_.tags)
      val hasEmptyTag = filterNoTag.contains("")

      subquery
        .select(subRoot.get(Key_.id))
        .where(
          cb.equal(subRoot.get(Key_.id), root.get(Key_.id)),
          subJoin.get(Tag_.name).`in`(filterNoTag),
        )

      queryBase.whereConditions.add(
        cb.not(cb.exists(subquery)),
      )
      if (hasEmptyTag) {
        queryBase.whereConditions.add(tagsJoin.get(Tag_.name).isNotNull)
      }
    }
  }

  private fun filterTask() {
    if (params.filterTaskNumber != null) {
      val translationTaskKeyJoin =
        queryBase.root
          .join(Key_.tasks, JoinType.LEFT)
      val translationTaskJoin =
        translationTaskKeyJoin
          .join(TaskKey_.task, JoinType.LEFT)

      queryBase.whereConditions.add(translationTaskJoin.get(Task_.number).`in`(params.filterTaskNumber))

      if (params.filterTaskKeysNotDone == true) {
        queryBase.whereConditions.add(translationTaskKeyJoin.get(TaskKey_.done).`in`(false))
      }

      if (params.filterTaskKeysDone == true) {
        queryBase.whereConditions.add(translationTaskKeyJoin.get(TaskKey_.done).`in`(true))
      }
    }
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
}
