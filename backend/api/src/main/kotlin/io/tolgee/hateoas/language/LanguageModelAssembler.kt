package io.tolgee.hateoas.language

import io.tolgee.api.v2.controllers.V2LanguagesController
import io.tolgee.model.views.LanguageView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class LanguageModelAssembler : RepresentationModelAssemblerSupport<LanguageView, LanguageModel>(
  V2LanguagesController::class.java,
  LanguageModel::class.java,
) {
  override fun toModel(view: LanguageView): LanguageModel {
    return LanguageModel(
      id = view.language.id,
      name = view.language.name ?: "",
      originalName = view.language.originalName,
      tag = view.language.tag,
      flagEmoji = view.language.flagEmoji,
      base = view.base,
    )
  }
}
