package io.tolgee.activity.activities.translation

import io.tolgee.activity.ActivityService
import io.tolgee.activity.activities.common.BaseTranslationsActivity
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.translation.TranslationComment
import org.springframework.stereotype.Component

@Component
class SetTranslationsActivity(
  activityService: ActivityService,
) : BaseTranslationsActivity(activityService) {

  override val type: String = "SET_TRANSLATIONS"

  override val metaModifier: (
    (
      meta: MutableMap<String, Any?>,
      activityModifiedEntity: ActivityModifiedEntity,
      entity: EntityWithId
    ) -> Unit
  )
    get() = { meta, activityModifiedEntity, entity ->
      super.metaModifier(meta, activityModifiedEntity, entity)

      if (entity is TranslationComment) {
        meta["translationCommentText"] = entity.text
      }
    }
}
