package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.snapshot.KeyMetaSnapshot

class KeyMetaSnapshotBuilder(
  val keySnapshotBuilder: KeySnapshotBuilder,
) : BaseEntityDataBuilder<KeyMetaSnapshot, KeyMetaSnapshotBuilder>() {
  override var self: KeyMetaSnapshot =
    KeyMetaSnapshot().apply {
      keySnapshot = keySnapshotBuilder.self
      keySnapshotBuilder.self.keyMetaSnapshot = this
    }
}
