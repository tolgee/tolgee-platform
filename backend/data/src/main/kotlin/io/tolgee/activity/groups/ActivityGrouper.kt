package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.model.activity.ActivityRevision
import org.springframework.context.ApplicationContext

class ActivityGrouper(
  activityRevision: ActivityRevision,
  modifiedEntities: ModifiedEntitiesType,
  applicationContext: ApplicationContext,
) {
  fun addToGroup() {
  }

  fun findGroupTypes() {
    val type = type ?: return

    ActivityGroupType.values().filter { it.matches }
  }

  private val ActivityGroupType.matches: Boolean
    get() {
      if (!this.sourceActivityTypes.contains(type)) {
        return false
      }
    }

  private val type = activityRevision.type
}
