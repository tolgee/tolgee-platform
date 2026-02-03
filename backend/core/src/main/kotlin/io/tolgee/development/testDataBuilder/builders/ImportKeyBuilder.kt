package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.key.KeyMeta

class ImportKeyBuilder(
  importFileBuilder: ImportFileBuilder,
) : EntityDataBuilder<ImportKey, ImportKeyBuilder> {
  class DATA {
    var meta: KeyMetaBuilder? = null
    val screenshots = mutableListOf<ScreenshotBuilder>()
  }

  val data = DATA()

  override var self: ImportKey = ImportKey("testKey", importFileBuilder.self)

  fun addMeta(ft: FT<KeyMeta>) {
    data.meta =
      KeyMetaBuilder(this).apply {
        ft(this.self)
      }
  }
}
