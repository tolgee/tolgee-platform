package io.tolgee.activity.activities.common

import io.tolgee.activity.ActivityService

abstract class BaseTranslationsActivity(
  activityService: ActivityService,
) : AllModificationsReturningActivity(
  activityService
) {
  override val metaModifier: (
    meta: MutableMap<String, Any?>,
    activityModifiedEntity: io.tolgee.model.activity.ActivityModifiedEntity,
    entity: io.tolgee.model.EntityWithId
  ) -> Unit = { meta, _, entity ->
    if (entity is io.tolgee.model.translation.Translation && meta.isEmpty()) {
      meta["keyName"] = entity.key.name
      meta["keyId"] = entity.key.id
      meta["languageId"] = entity.language.id
      meta["languageTag"] = entity.language.tag
    }
  }
}
