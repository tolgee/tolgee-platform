package io.tolgee.activity.activities.translation.translationComment

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.AllModificationsReturningActivity
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.translation.TranslationComment

abstract class BaseTranslationCommentActivity(activityService: ActivityService) : AllModificationsReturningActivity(
  activityService
) {
  override val metaModifier: (
    (
      meta: MutableMap<String, Any?>,
      activityModifiedEntity: ActivityModifiedEntity,
      entity: EntityWithId
    ) -> Unit
  )? = { meta: MutableMap<String, Any?>, activityModifiedEntity: ActivityModifiedEntity, entity: EntityWithId ->
    if (entity is TranslationComment) {
      meta["translationId"] = entity.translation.id
      meta["translationText"] = entity.translation.text
      meta["keyId"] = entity.translation.key.id
    }
  }
}
