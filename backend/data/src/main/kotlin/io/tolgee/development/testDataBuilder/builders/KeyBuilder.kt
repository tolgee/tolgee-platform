package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation

class KeyBuilder(
  val projectBuilder: ProjectBuilder
) : BaseEntityDataBuilder<Key, KeyBuilder>() {

  class DATA {
    var meta: KeyMetaBuilder? = null
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

  fun setNamespace(name: String?): NamespaceBuilder? {
    if (name == null) {
      this.self.namespace = null
      return null
    }
    val nsBuilder = projectBuilder.data.namespaces.singleOrNull { it.self.name === name }
      ?: projectBuilder.addNamespace { this.name = name }
    this.self.namespace = nsBuilder.self
    return nsBuilder
  }

  fun addScreenshot(ft: Screenshot.(reference: KeyScreenshotReference) -> Unit): ScreenshotBuilder {
    val screenshotBuilder = projectBuilder.addScreenshot {}
    val reference = projectBuilder.addScreenshotReference {
      key = this@KeyBuilder.self
      screenshot = screenshotBuilder.self
    }
    ft(screenshotBuilder.self, reference.self)
    return screenshotBuilder
  }

  fun addTranslation(languageTag: String, text: String?) {
    addTranslation {
      this.language = projectBuilder.getLanguageByTag(languageTag)!!.self
      this.text = text
    }
  }
}
