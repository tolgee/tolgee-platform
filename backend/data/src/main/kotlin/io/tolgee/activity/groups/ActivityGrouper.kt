package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.activity.groups.matchers.modifiedEntity.StoringContext
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

class ActivityGrouper(
  private val activityRevision: ActivityRevision,
  private val modifiedEntities: ModifiedEntitiesType,
  private val applicationContext: ApplicationContext,
) {
  fun addToGroup() {
    ActivityGroupType.entries.forEach { type ->
      val matchingEntities = type.matchingEntities
      if (matchingEntities.isEmpty()) {
        return@forEach
      }
      matchingEntities.groupBy { it.branchId }.forEach { (branchId, entities) ->
        val matchingStrings =
          entities
            .map { type.matchingStringProvider?.provide(it.getStoringContext()) }
            .toSet()
        getActivityGroupIds(type, matchingStrings, branchId).forEach { group ->
          addToGroup(group.value)
        }
      }
    }
  }

  private fun addToGroup(groupId: Long) {
    entityManager
      .createNativeQuery(
        """
      insert into activity_revision_activity_groups (activity_revisions_id, activity_groups_id) 
      values (:activityRevisionId, :activityGroupId)
    """,
      ).setParameter("activityRevisionId", activityRevision.id)
      .setParameter("activityGroupId", groupId)
      .executeUpdate()
  }

  private fun getActivityGroupIds(
    type: ActivityGroupType,
    matchingStrings: Set<String?>,
    branchId: Long?,
  ): Map<String?, Long> {
    return activityGroupService
      .getOrCreateCurrentActivityGroupDto(
        type,
        matchingStrings,
        activityRevision.projectId,
        activityRevision.authorId,
        branchId,
      ).mapValues { it.value.id }
  }

  private val ActivityGroupType.matchingEntities: List<ActivityModifiedEntity>
    get() {
      if (!this.sourceActivityTypes.contains(type)) {
        return emptyList()
      }

      return modifiedEntities.values.flatMap {
        it.values.filter { entity -> entityMatches(entity) }
      }
    }

  private fun ActivityGroupType.entityMatches(entity: ActivityModifiedEntity): Boolean {
    return this.matcher?.match(entity.getStoringContext()) ?: true
  }

  private fun ActivityModifiedEntity.getStoringContext(): StoringContext {
    return StoringContext(this, activityRevision)
  }

  private val type = activityRevision.type

  private val activityGroupService by lazy {
    applicationContext.getBean(ActivityGroupService::class.java)
  }

  private val entityManager: EntityManager by lazy {
    applicationContext.getBean(EntityManager::class.java)
  }
}
