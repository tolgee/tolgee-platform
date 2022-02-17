package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation

class KeyBuilder(
  val projectBuilder: ProjectBuilder
) : BaseEntityDataBuilder<Key, KeyBuilder>() {

  class DATA {
    var meta: KeyMetaBuilder? = null
    var screenshots = mutableListOf<ScreenshotBuilder>()
  }

  val data = DATA()

  override var self: Key = Key().also {
    it.project = projectBuilder.self
  }

  fun addTranslation(ft: FT<Translation>): TranslationBuilder {
    val builder = TranslationBuilder(projectBuilder).apply {
      self.key = this@KeyBuilder.self
    }
    return addOperation(projectBuilder.data.translations, builder, ft)
  }

  fun addMeta(ft: FT<KeyMeta>) {
    data.meta = KeyMetaBuilder(keyBuilder = this).apply { ft(this.self) }
  }

  fun addScreenshot(ft: FT<Screenshot>) = addOperation(data.screenshots, ft)
}
