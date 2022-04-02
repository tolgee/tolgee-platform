package io.tolgee.activity.activities.common

import io.tolgee.activity.ActivityService
import io.tolgee.model.activity.ActivityModifiedEntity

abstract class AllModificationsReturningActivity(
  private val activityService: ActivityService
) : Activity {

  override fun getModifications(revisionIds: Collection<Long>): Map<Long, List<ActivityModifiedEntity>> {
    return activityService.getModifiedEntitiesForEachRevision(revisionIds)
  }
}
