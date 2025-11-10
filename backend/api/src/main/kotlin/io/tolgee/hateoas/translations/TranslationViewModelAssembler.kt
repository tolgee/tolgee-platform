package io.tolgee.hateoas.translations

import io.tolgee.api.v2.controllers.translation.TranslationsController
import io.tolgee.hateoas.label.LabelModelAssembler
import io.tolgee.hateoas.translations.suggestions.TranslationSuggestionSimpleModelAssembler
import io.tolgee.model.views.TranslationView
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationViewModelAssembler(
  private val labelModelAssembler: LabelModelAssembler,
  private val translationSuggestionSimpleModelAssembler: TranslationSuggestionSimpleModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationView, TranslationViewModel>(
    TranslationsController::class.java,
    TranslationViewModel::class.java,
  ) {
  override fun toModel(view: TranslationView): TranslationViewModel {
    return TranslationViewModel(
      id = view.id,
      text = view.text,
      state = view.state,
      auto = view.auto,
      outdated = view.outdated,
      mtProvider = view.mtProvider,
      commentCount = view.commentCount,
      unresolvedCommentCount = view.unresolvedCommentCount,
      labels = view.labels.map { labelModelAssembler.toModel(it) },
      activeSuggestionCount = view.activeSuggestionCount,
      totalSuggestionCount = view.totalSuggestionCount,
      suggestions = view.suggestions?.map { translationSuggestionSimpleModelAssembler.toModel(it) },
    )
  }
}
