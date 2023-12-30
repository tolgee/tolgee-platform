package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.KeysDistance

class KeysDistanceBuilder(
  val projectBuilder: ProjectBuilder,
  val key1: Key,
  val key2: Key,
) : EntityDataBuilder<KeysDistance, KeysDistanceBuilder> {
  override var self: KeysDistance =
    KeysDistance().apply {
      project = projectBuilder.self
      new = true
    }
}
