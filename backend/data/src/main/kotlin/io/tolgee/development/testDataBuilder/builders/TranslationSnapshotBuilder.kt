package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.snapshot.TranslationSnapshot

class TranslationSnapshotBuilder(
  val keySnapshotBuilder: KeySnapshotBuilder,
) : BaseEntityDataBuilder<TranslationSnapshot, TranslationSnapshotBuilder>() {
  override var self: TranslationSnapshot =
    TranslationSnapshot(language = "", value = "").apply {
      keySnapshot = keySnapshotBuilder.self
      keySnapshotBuilder.self.translations.add(this)
    }
}
