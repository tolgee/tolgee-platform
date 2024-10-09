package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.model.views.KeyTaskView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class KeyTaskViewModelAssembler :
  RepresentationModelAssemblerSupport<KeyTaskView, KeyTaskViewModel>(
    TranslationsController::class.java,
    KeyTaskViewModel::class.java,
  ) {
  override fun toModel(view: KeyTaskView): KeyTaskViewModel {
    return KeyTaskViewModel(
      number = view.number,
      languageId = view.languageId,
      languageTag = view.languageTag,
      done = view.done,
      userAssigned = view.userAssigned,
      type = view.type,
    )
  }
}
