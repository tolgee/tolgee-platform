package io.tolgee.activity.holders

import io.tolgee.activity.activities.common.Activity
import io.tolgee.model.activity.ActivityRevision

open class ActivityHolder {
  open var activity: Activity? = null

  open var activityRevision: ActivityRevision? = null
}
