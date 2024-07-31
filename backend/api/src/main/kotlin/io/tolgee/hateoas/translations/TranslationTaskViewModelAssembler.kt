package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.model.views.TranslationTaskView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationTaskViewModelAssembler :
  RepresentationModelAssemblerSupport<TranslationTaskView, TranslationTaskViewModel>(
    TranslationsController::class.java,
    TranslationTaskViewModel::class.java,
  ) {
  override fun toModel(view: TranslationTaskView): TranslationTaskViewModel {
    return TranslationTaskViewModel(
      id = view.id,
      done = view.done,
      userAssigned = view.userAssigned,
      type = view.type,
    )
  }
}
