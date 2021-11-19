package io.tolgee.api.v2.hateoas.translations

import io.tolgee.api.v2.controllers.translation.V2TranslationsController
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.api.v2.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.model.views.KeyWithTranslationsView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyWithTranslationsModelAssembler(
  private val translationViewModelAssembler: TranslationViewModelAssembler,
  private val tagModelAssembler: TagModelAssembler,
  private val screenshotModelAssembler: ScreenshotModelAssembler
) : RepresentationModelAssemblerSupport<KeyWithTranslationsView, KeyWithTranslationsModel>(
  V2TranslationsController::class.java, KeyWithTranslationsModel::class.java
) {
  override fun toModel(view: KeyWithTranslationsView) = KeyWithTranslationsModel(
    keyId = view.keyId,
    keyName = view.keyName,
    keyTags = view.keyTags.map { tagModelAssembler.toModel(it) },
    translations = view.translations.map {
      it.key to translationViewModelAssembler.toModel(it.value)
    }.toMap(),
    screenshotCount = view.screenshotCount,
    screenshots = view.screenshots?.map {
      screenshotModelAssembler.toModel(it)
    }
  )
}
