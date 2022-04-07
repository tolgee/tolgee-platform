package io.tolgee.activity.activities.key

import io.tolgee.activity.activities.common.Activity
import io.tolgee.model.EntityWithId
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Component

@Component
class ComplexEditActivity() : Activity {
  override val type: String = "COMPLEX_EDIT_ACTIVITY"

  override val metaModifier: (
  (
    meta: MutableMap<String, Any?>,
    activityModifiedEntity: ActivityModifiedEntity,
    entity: EntityWithId
  ) -> Unit
  )? = { meta: MutableMap<String, Any?>, activityModifiedEntity: ActivityModifiedEntity, entity: EntityWithId ->
    if (entity is Translation) {
      val tags = meta.computeIfAbsent("modifiedTranslationTags") {
        mutableListOf<String>()
      } as? MutableList<String>
      tags?.add(entity.language.tag)
    }
  }

}
