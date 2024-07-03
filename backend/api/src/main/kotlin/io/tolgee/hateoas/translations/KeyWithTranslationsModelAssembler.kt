package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.api.v2.hateoas.invitation.TagModelAssembler
import io.tolgee.hateoas.screenshot.ScreenshotModelAssembler
import io.tolgee.model.views.KeyWithTranslationsView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyWithTranslationsModelAssembler(
  private val translationViewModelAssembler: TranslationViewModelAssembler,
  private val tagModelAssembler: TagModelAssembler,
  private val screenshotModelAssembler: ScreenshotModelAssembler,
  private val translationTaskViewModelAssembler: KeyTaskViewModelAssembler,
) : RepresentationModelAssemblerSupport<KeyWithTranslationsView, KeyWithTranslationsModel>(
    TranslationsController::class.java,
    KeyWithTranslationsModel::class.java,
  ) {
  override fun toModel(view: KeyWithTranslationsView) =
    KeyWithTranslationsModel(
      keyId = view.keyId,
      keyName = view.keyName,
      keyNamespaceId = view.keyNamespaceId,
      keyIsPlural = view.keyIsPlural,
      keyPluralArgName = view.keyPluralArgName,
      keyNamespace = view.keyNamespace,
      keyDescription = view.keyDescription,
      keyTags = view.keyTags.map { tagModelAssembler.toModel(it) },
      contextPresent = view.contextPresent,
      translations =
        view.translations.map {
          it.key to translationViewModelAssembler.toModel(it.value)
        }.toMap(),
      screenshotCount = view.screenshotCount,
      screenshots =
        view.screenshots?.map {
          screenshotModelAssembler.toModel(it)
        },
      tasks =
        view.tasks?.map {
          translationTaskViewModelAssembler.toModel(it)
        },
    )
}
