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
    val groupTypes = findGroupTypes()
    groupTypes.forEach { (type, matchingStrings) ->
      getActivityGroupIds(type, matchingStrings).forEach { group ->
        addToGroup(group.value)
      }
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

  private fun getActivityGroupIds(
    type: ActivityGroupType,
    matchingStrings: Set<String?>,
  ): Map<String?, Long> {
    return activityGroupService.getOrCreateCurrentActivityGroupDto(
      type,
      matchingStrings,
      activityRevision.projectId,
      activityRevision.authorId,
    ).mapValues { it.value.id }
  }

  private fun findGroupTypes(): Map<ActivityGroupType, Set<String?>> {
    return ActivityGroupType.entries.mapNotNull { activityGroupType ->
      val matchingEntities = activityGroupType.matchingEntities
      if (matchingEntities.isEmpty()) {
        return@mapNotNull null
      }
      activityGroupType to
        activityGroupType.matchingEntities.map { entity ->
          activityGroupType.matchingStringProvider?.provide(
            entity.getStoringContext(),
          )
        }.toSet()
    }.toMap()
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
