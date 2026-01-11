package io.tolgee.activity.projectActivity

import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityDescribingEntity_
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision_
import jakarta.persistence.EntityManager

class RelationDataProvider(
  private val entityManager: EntityManager,
  private val rawModifiedEntities: Iterable<ActivityModifiedEntity>,
) {
  private lateinit var allRelationData: MutableMap<Long, MutableList<ActivityDescribingEntity>>

  fun provide(): MutableMap<Long, MutableList<ActivityDescribingEntity>> {
    var missing: Set<Triple<Long, String, Long>> = getInitialMissingRelationData()

    allRelationData =
      getRelationsForRevisions(missing)
        .groupBy { it.activityRevision.id }
        .mapValues { it.value.toMutableList() }
        .toMutableMap()

    val retrieved = missing.toMutableSet()

    missing = getMissingRelationData()

    while (!retrieved.containsAll(missing)) {
      getRelationsForRevisions(missing).map {
        allRelationData.computeIfAbsent(it.activityRevision.id) { mutableListOf() }.add(it)
      }
      retrieved.addAll(missing)
      missing = getMissingRelationData()
    }

    return allRelationData
  }

  private fun getInitialMissingRelationData() =
    rawModifiedEntities
      .asSequence()
      .flatMap {
        it.describingRelations?.map { dr ->
          Triple(it.activityRevision.id, dr.value.entityClass, dr.value.entityId)
        } ?: listOf()
      }.toMutableSet()

  private fun getMissingRelationData(): Set<Triple<Long, String, Long>> {
    val missing =
      allRelationData.entries
        .flatMap { entry ->
          entry.value.flatMap { relation ->
            relation.describingRelations?.values?.map {
              Triple(
                entry.key,
                it.entityClass,
                it.entityId,
              )
            } ?: listOf()
          }
        }.toMutableSet()

    missing.removeIf { missingItem ->
      allRelationData[missingItem.first]?.any {
        it.entityClass == missingItem.second && it.entityId == missingItem.third
      } == true
    }

    return missing
  }

  private fun getRelationsForRevisions(
    missing: Set<Triple<Long, String, Long>>,
  ): MutableList<ActivityDescribingEntity> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(ActivityDescribingEntity::class.java)
    val root = query.from(ActivityDescribingEntity::class.java)
    val revision = root.join(ActivityDescribingEntity_.activityRevision)

    missing
      .map { (revisionId, entityClass, entityId) ->
        cb.and(
          cb.equal(revision.get(ActivityRevision_.id), revisionId),
          cb.equal(root.get(ActivityDescribingEntity_.entityClass), entityClass),
          cb.equal(root.get(ActivityDescribingEntity_.entityId), entityId),
        )
      }.let {
        query.where(cb.or(*it.toTypedArray()))
      }

    return entityManager.createQuery(query).resultList
  }
}
