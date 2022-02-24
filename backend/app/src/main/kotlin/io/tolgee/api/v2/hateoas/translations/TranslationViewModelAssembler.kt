package io.tolgee.api.v2.hateoas.translations

import io.tolgee.api.v2.controllers.translation.V2TranslationsController
import io.tolgee.model.views.TranslationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationViewModelAssembler : RepresentationModelAssemblerSupport<TranslationView, TranslationViewModel>(
  V2TranslationsController::class.java, TranslationViewModel::class.java
) {
  override fun toModel(view: TranslationView): TranslationViewModel {
    return TranslationViewModel(
      id = view.id,
      text = view.text,
      state = view.state,
      auto = view.auto,
      mtProvider = view.mtProvider,
      commentCount = view.commentCount,
      unresolvedCommentCount = view.unresolvedCommentCount
    )
  }
}
