package io.tolgee.activity.activities

import io.tolgee.model.Activity
import io.tolgee.model.EntityWithId
import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Component

@Component
class SetTranslationActivity : ActivityManager {
  override val metaModifier: (
    meta: MutableMap<String, Any?>,
    activity: Activity,
    entity: EntityWithId
  ) -> Unit = { meta, _, entity ->
    if (entity is Translation && meta.isEmpty()) {
      meta["keyName"] = entity.key.name
      meta["keyId"] = entity.key.id
      meta["languageId"] = entity.language.id
      meta["languageTag"] = entity.language.tag
    }
  }
}
