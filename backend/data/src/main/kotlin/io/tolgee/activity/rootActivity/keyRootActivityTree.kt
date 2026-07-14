package io.tolgee.activity.rootActivity

import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation

val KeyActivityTreeDefinitionItem =
  ActivityTreeDefinitionItem(
    entityClass = Key::class,
    children =
      mapOf(
        "translations" to
          ActivityTreeDefinitionItem(
            describingField = "key",
            entityClass = Translation::class,
          ),
        "keyMeta" to
          ActivityTreeDefinitionItem(
            describingField = "key",
            entityClass = KeyMeta::class,
          ),
      ),
  )
