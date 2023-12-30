package io.tolgee.events

import io.tolgee.activity.ModifiedEntitiesType
import io.tolgee.activity.UtmData
import io.tolgee.model.activity.ActivityRevision

class OnProjectActivityEvent(
  val activityRevision: ActivityRevision,
  val modifiedEntities: ModifiedEntitiesType,
  val organizationId: Long?,
  var utmData: UtmData,
  var sdkInfo: Map<String, String?>?,
)
