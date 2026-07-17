package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot

class KeySnapshotBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<KeySnapshot, KeySnapshotBuilder>() {
  class DATA {
    val translationSnapshots = mutableListOf<TranslationSnapshotBuilder>()
    val keyMetaSnapshots = mutableListOf<KeyMetaSnapshotBuilder>()
  }

  val data = DATA()

  override val self: KeySnapshot =
    KeySnapshot().apply {
      project = projectBuilder.self
    }

  fun addTranslationSnapshot(ft: FT<TranslationSnapshot>) = addOperation(data.translationSnapshots, ft)

  fun addKeyMetaSnapshot(ft: FT<KeyMetaSnapshot>) = addOperation(data.keyMetaSnapshots, ft)
}
