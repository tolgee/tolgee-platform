package io.tolgee.activity.holders

import io.tolgee.activity.activities.ActivityManager
import io.tolgee.model.ActivityRevision

open class ActivityHolder {
  open var manager: ActivityManager? = null

  open var activityRevision: ActivityRevision? = null
}
