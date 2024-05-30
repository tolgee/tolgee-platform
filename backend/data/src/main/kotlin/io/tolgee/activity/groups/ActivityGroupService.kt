package io.tolgee.activity.groups

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.model.activity.ActivityRevision
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

@Service
class ActivityGroupService(
  private val applicationContext: ApplicationContext,
) {
  fun addToGroup(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  ) {
    ActivityGrouper(activityRevision, modifiedEntities, applicationContext).addToGroup()
  }
}
