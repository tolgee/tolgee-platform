package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.activity.data.PropertyModification
import io.tolgee.activity.groups.matchers.ActivityGroupValueMatcher
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class ActivityGrouper(
  private val activityRevision: ActivityRevision,
  private val modifiedEntities: ModifiedEntitiesType,
  private val applicationContext: ApplicationContext,
) {
  fun addToGroup() {
    val groupTypes = findGroupTypes()
    groupTypes.forEach {
      val groupIdToAddTo = getActivityGroupIds(it)
      addToGroup(groupIdToAddTo)
    }
  }

  private fun addToGroup(groupId: Long) {
    entityManager.createNativeQuery(
      """
      insert into activity_revision_activity_groups (activity_revisions_id, activity_groups_id) 
      values (:activityRevisionId, :activityGroupId)
    """,
    )
      .setParameter("activityRevisionId", activityRevision.id)
      .setParameter("activityGroupId", groupId)
      .executeUpdate()
  }

  private fun getActivityGroupIds(type: ActivityGroupType): Long {
    return activityGroupService.getOrCreateCurrentActivityGroupDtos(
      type,
      activityRevision.projectId,
      activityRevision.authorId,
    ).id
  }

  private fun findGroupTypes(): List<ActivityGroupType> {
    return ActivityGroupType.entries.filter { it.matches }
  }

  private val ActivityGroupType.matches: Boolean
    get() {
      if (!this.sourceActivityTypes.contains(type)) {
        return false
      }

      return modifiedEntities.any { modifiedEntity ->
        this.modifications.any { definition ->
          definition.matchesModifiedEntity(modifiedEntity)
        }
      }
    }

  private fun GroupEntityModificationDefinition<*>.matchesModifiedEntity(entityEntry: ModifiedEntityType): Boolean {
    if (entityEntry.key != entityClass) {
      return false
    }

    val isAnyWithAllowedType = entityEntry.value.any { it.value.revisionType in revisionTypes }
    if (!isAnyWithAllowedType) {
      return false
    }

    val anyWithModifiedColumn =
      entityEntry.value.values.any { entity ->
        entity.modifications.any { modification ->
          modificationProps?.any { it.name == modification.key } ?: true
        }
      }

    if (!anyWithModifiedColumn) {
      return false
    }

    if (!allowedValues.matchesEntityModification(entityEntry)) {
      return false
    }

    val isDenied = deniedValues?.matchesEntityModification(entityEntry) ?: false
    return !isDenied
  }

  private fun Map<out KProperty1<*, *>, Any?>?.matchesEntityModification(entry: ModifiedEntityType): Boolean {
    return entry.value.values.any { entity ->
      entity.modifications.any { modification ->
        this?.all { compareValue(it.value, modification.value) && it.key.name == modification.key } ?: true
      }
    }
  }

  private fun compareValue(
    value: Any?,
    value1: PropertyModification,
  ): Boolean {
    return when (value) {
      is ActivityGroupValueMatcher -> value.match(value1.new)
      else -> value == value1.new
    }
  }

  private val type = activityRevision.type

  private val activityGroupService by lazy {
    applicationContext.getBean(ActivityGroupService::class.java)
  }

  private val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }
}

private typealias ModifiedEntityType = Map.Entry<KClass<out EntityWithId>, MutableMap<Long, ActivityModifiedEntity>>
