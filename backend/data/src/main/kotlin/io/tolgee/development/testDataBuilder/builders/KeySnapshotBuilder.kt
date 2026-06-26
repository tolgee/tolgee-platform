package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.snapshot.KeySnapshot

class KeySnapshotBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<KeySnapshot, KeySnapshotBuilder>() {
  override val self: KeySnapshot =
    KeySnapshot().apply {
      project = projectBuilder.self
    }
}
