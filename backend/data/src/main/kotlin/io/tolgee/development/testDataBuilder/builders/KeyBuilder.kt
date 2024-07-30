package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation
import io.tolgee.util.ImageConverter
import org.springframework.core.io.Resource

class KeyBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<Key, KeyBuilder>() {
  class DATA {
    var meta: KeyMetaBuilder? = null
  }

  val data = DATA()

  override var self: Key =
    Key().also {
      it.project = projectBuilder.self
    }

  fun addTranslation(ft: FT<Translation>): TranslationBuilder {
    val builder =
      TranslationBuilder(projectBuilder).apply {
        self.key = this@KeyBuilder.self
      }
    return addOperation(projectBuilder.data.translations, builder, ft)
  }

  fun addMeta(ft: FT<KeyMeta>): KeyMetaBuilder {
    data.meta = KeyMetaBuilder(keyBuilder = this).apply { ft(this.self) }
    return data.meta!!
  }

  fun setNamespace(name: String?): NamespaceBuilder? {
    if (name == null) {
      this.self.namespace = null
      return null
    }
    val nsBuilder =
      projectBuilder.data.namespaces.singleOrNull { it.self.name === name }
        ?: projectBuilder.addNamespace { this.name = name }
    this.self.namespace = nsBuilder.self
    return nsBuilder
  }

  fun addScreenshot(
    file: Resource? = null,
    ft: Screenshot.(reference: KeyScreenshotReference) -> Unit,
  ): ScreenshotBuilder {
    val converter = file?.let { ImageConverter(file.inputStream) }
    val image = converter?.getImage()
    val thumbnail = converter?.getThumbnail()

    val screenshotBuilder =
      projectBuilder.addScreenshot {
        width = converter?.targetDimension?.width ?: 0
        height = converter?.targetDimension?.height ?: 0
      }

    screenshotBuilder.image = image
    screenshotBuilder.thumbnail = thumbnail

    val reference =
      projectBuilder.addScreenshotReference {
        key = this@KeyBuilder.self
        screenshot = screenshotBuilder.self
      }

    ft(screenshotBuilder.self, reference.self)
    return screenshotBuilder
  }

  fun addTranslation(
    languageTag: String,
    text: String?,
  ): TranslationBuilder {
    return addTranslation {
      this.language = projectBuilder.getLanguageByTag(languageTag)!!.self
      this.text = text
    }
  }

  fun setDescription(description: String) {
    val meta = this.data.meta ?: addMeta { }
    meta.self.description = description
  }

  fun addTag(name: String): Tag {
    val meta = this.data.meta ?: addMeta { }

    val tags =
      projectBuilder.data.keys
        .mapNotNull { it.data.meta?.self?.tags }.flatten().filter { it.name == name }.distinct()

    if (tags.size > 1) {
      throw IllegalStateException("More than one tag with name $name in the project")
    }

    val tag =
      tags.firstOrNull() ?: Tag().apply {
        this.name = name
        this.project = projectBuilder.self
      }

    meta.self.tags.add(tag)
    return tag
  }
}
