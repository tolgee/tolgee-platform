package io.tolgee.hateoas.key

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.hateoas.translations.TranslationModelAssembler
import io.tolgee.model.key.Key
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyWithDataModelAssembler(
  private val tagModelAssembler: TagModelAssembler,
  private val translationModelAssembler: TranslationModelAssembler,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
) : RepresentationModelAssemblerSupport<Key, KeyWithDataModel>(
    TranslationsController::class.java,
    KeyWithDataModel::class.java,
  ) {
  override fun toModel(entity: Key) =
    KeyWithDataModel(
      id = entity.id,
      name = entity.name,
      namespace = entity.namespace?.name,
      translations =
        entity.translations
          .map {
            it.language.tag to translationModelAssembler.toModel(it)
          }.toMap(),
      tags =
        entity.keyMeta
          ?.tags
          ?.map { tagModelAssembler.toModel(it) }
          ?.toSet() ?: setOf(),
      screenshots = entity.keyScreenshotReferences.map { it.screenshot }.map { screenshotModelAssembler.toModel(it) },
      description = entity.keyMeta?.description,
      isPlural = entity.isPlural,
      pluralArgName = entity.pluralArgName,
      custom = entity.keyMeta?.custom ?: mapOf(),
    )
}
