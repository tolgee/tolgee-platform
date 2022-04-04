package io.tolgee.activity.activities.translation.translationComment

import io.tolgee.activity.ActivityService
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Component

@Component
class TranslationCommentSetStateActivity(activityService: ActivityService) : BaseTranslationCommentActivity(
  activityService
) {
  override val type: String = "TRANSLATION_COMMENT_SET_STATE"

  override val metaModifier: (
    (
      meta: MutableMap<String, Any?>,
      activityModifiedEntity: ActivityModifiedEntity,
      entity: EntityWithId
    ) -> Unit
  )?
    get() = { meta, activityModifiedEntity, entity ->
      super.metaModifier?.let {
        it(meta, activityModifiedEntity, entity)
      }

      if (entity is Translation) {
        meta["translationText"] = entity.text
      }
    }
}
