package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.activity.groups.matchers.modifiedEntity.StoringContext
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
    groupTypes.forEach {
      val groupIdToAddTo = getActivityGroupId(it)
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

  private fun getActivityGroupId(type: ActivityGroupType): Long {
    return activityGroupService.getOrCreateCurrentActivityGroupDto(
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

      return modifiedEntities.any { modifiedEntityEntry ->
        modifiedEntityEntry.value.values.any { modifiedEntity ->
          this.matcher?.match(StoringContext(modifiedEntity)) ?: true
        }
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
