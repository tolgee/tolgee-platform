package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityDescribingEntity_
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityModifiedEntity_
import io.tolgee.model.activity.ActivityRevision_
import io.tolgee.model.key.KeyMeta_
import io.tolgee.model.key.Key_
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
import java.util.*

class QueryGlobalFiltering(
  private val params: TranslationFilters,
  private val queryBase: QueryBase<*>,
  private val cb: CriteriaBuilder,
  private val entityManager: EntityManager,
) {
  fun apply() {
    filterTag()
    filterNamespace()
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
    if (params.filterHasNoScreenshot) {
      queryBase.whereConditions.add(cb.lt(queryBase.screenshotCountExpression, 1))
    }
  }

  private fun filterHasScreenshot() {
    if (params.filterHasScreenshot) {
      queryBase.whereConditions.add(cb.gt(queryBase.screenshotCountExpression, 0))
    }
  }

  private fun filterTranslatedAny() {
    if (params.filterTranslatedAny) {
      val predicates =
        queryBase.translationsTextFields
          .map { with(queryBase) { it.isNotNullOrBlank } }
          .toTypedArray()
      queryBase.whereConditions.add(cb.or(*predicates))
    }
  }

  private fun filterUntranslatedAny() {
    if (params.filterUntranslatedAny) {
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
    val filterNamespace = getFilterNamespace()
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

  private fun getFilterNamespace(): List<String>? {
    val filterNamespace = params.filterNamespace
    if (filterNamespace != null && filterNamespace.isEmpty()) {
      return listOf("")
    }
    return filterNamespace
  }

  private fun filterTag() {
    if (params.filterTag != null) {
      val keyMetaJoin = queryBase.root.join(Key_.keyMeta, JoinType.LEFT)
      val tagsJoin = keyMetaJoin.join(KeyMeta_.tags, JoinType.LEFT)
      queryBase.whereConditions.add(tagsJoin.get(Tag_.name).`in`(params.filterTag))
    }
  }

  private fun filterTask() {
    if (params.filterTaskNumber != null) {
      val translationTaskJoin =
        queryBase.root
          .join(Key_.tasks, JoinType.LEFT)
          .join(TaskKey_.task, JoinType.LEFT)

      queryBase.whereConditions.add(translationTaskJoin.get(Task_.number).`in`(params.filterTaskNumber))
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
          deRoot.get(
            ActivityDescribingEntity_.activityRevision,
          ).get(ActivityRevision_.id).`in`(params.filterRevisionId),
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
