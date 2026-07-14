package io.tolgee.activity.groups.matchers.modifiedEntity

import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision

/**
 * This is all the context that is provided when storing the activity data
 */
class StoringContext(
  val modifiedEntity: ActivityModifiedEntity,
  val activityRevision: ActivityRevision,
)
