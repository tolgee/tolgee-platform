package io.tolgee.hateoas.key

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.hateoas.translations.TranslationModelAssembler
import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyWithScreenshotsModelAssembler(
  private val tagModelAssembler: TagModelAssembler,
  private val translationModelAssembler: TranslationModelAssembler,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
) : RepresentationModelAssemblerSupport<Pair<Key, List<Screenshot>>, KeyWithDataModel>(
    TranslationsController::class.java,
    KeyWithDataModel::class.java,
  ) {
  override fun toModel(data: Pair<Key, List<Screenshot>>): KeyWithDataModel {
    val (entity, screenshots) = data
    return KeyWithDataModel(
      id = entity.id,
      name = entity.name,
      namespace = entity.namespace?.name,
      translations =
        entity.translations.associate {
          it.language.tag to translationModelAssembler.toModel(it)
        },
      tags =
        entity.keyMeta
          ?.tags
          ?.map { tagModelAssembler.toModel(it) }
          ?.toSet() ?: setOf(),
      screenshots = screenshots.map { screenshotModelAssembler.toModel(it) },
      description = entity.keyMeta?.description,
      isPlural = entity.isPlural,
      pluralArgName = entity.pluralArgName,
      custom = entity.keyMeta?.custom ?: mapOf(),
    )
  }
}
