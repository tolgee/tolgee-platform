package io.tolgee.model.batch.params

import io.tolgee.activity.data.ActivityType
import java.io.Serializable

class ActivityNotificationJobParams : Serializable {
  var projectId: Long? = null
  var originatingUserId: Long? = null
  var entityId: Long? = null
  var activityType: ActivityType? = null
}
