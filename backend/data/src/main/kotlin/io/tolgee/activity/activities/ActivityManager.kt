package io.tolgee.activity.activities

import io.tolgee.model.Activity
import io.tolgee.model.EntityWithId

interface ActivityManager {
  val metaModifier: (meta: MutableMap<String, Any?>, activity: Activity, entity: EntityWithId) -> Unit
}
