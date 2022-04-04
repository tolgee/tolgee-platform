package io.tolgee.activity.activities.key

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.AllModificationsReturningActivity
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import org.springframework.stereotype.Component

@Component
class CreateKeyActivity(
  activityService: ActivityService,
) : AllModificationsReturningActivity(activityService) {
  override val type: String = "CREATE_KEY"

  override val metaModifier: (
    (
      meta: MutableMap<String, Any?>,
      activityModifiedEntity: ActivityModifiedEntity,
      entity: EntityWithId
    ) -> Unit
  )? = null
}
