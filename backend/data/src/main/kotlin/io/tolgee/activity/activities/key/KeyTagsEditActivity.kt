package io.tolgee.activity.activities.key

import io.tolgee.activity.activities.common.Activity
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import org.springframework.stereotype.Component

@Component
class KeyTagsEditActivity() : Activity {
  override val type: String = "KEY_TAGS_EDIT"

  override val metaModifier: (
  (
    meta: MutableMap<String, Any?>,
    activityModifiedEntity: ActivityModifiedEntity,
    entity: EntityWithId
  ) -> Unit
  )? = null

}
