package io.tolgee.activity.activities.common

import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity

interface Activity {
  val type: String

  val metaModifier: (
  (
    meta: MutableMap<String, Any?>,
    activityModifiedEntity: ActivityModifiedEntity,
    entity: EntityWithId
  ) -> Unit
  )?
}
