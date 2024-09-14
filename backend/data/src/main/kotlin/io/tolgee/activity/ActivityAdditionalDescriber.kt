package io.tolgee.activity

import io.tolgee.model.activity.ActivityRevision

interface ActivityAdditionalDescriber {
  fun describe(
    activityRevision: ActivityRevision,
    modifiedEntities: ModifiedEntitiesType,
  )
}
