package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.key.KeyMeta

class KeyMetaBuilder(
  importKeyBuilder: ImportKeyBuilder? = null,
  keyBuilder: KeyBuilder? = null,
) : EntityDataBuilder<KeyMeta, KeyMetaBuilder> {
  override var self: KeyMeta =
    KeyMeta(
      key = keyBuilder?.self,
      importKey = importKeyBuilder?.self,
    ).also {
      keyBuilder?.self {
        keyMeta = it
      }
      importKeyBuilder?.self {
        keyMeta = it
      }
    }
}
