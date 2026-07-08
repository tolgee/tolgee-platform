package io.tolgee.api.v2.controllers.translation

import io.tolgee.dtos.request.translation.TranslationFilters
import org.springframework.beans.propertyeditors.CustomCollectionEditor
import org.springframework.web.bind.WebDataBinder

/**
 * Spring splits a single `?param=a,b` query value into `["a", "b"]` for `List` fields by
 * default. The params registered here carry free text that may contain commas, so every
 * controller binding [TranslationFilters] (or a subclass) must apply this customization.
 */
object TranslationFiltersBindingCustomizer {
  private val verbatimListParams =
    listOf(
      TranslationFilters::filterKeyName.name,
      TranslationFilters::filterKeyPattern.name,
      TranslationFilters::filterNoKeyPattern.name,
      TranslationFilters::filterDescriptionPattern.name,
      TranslationFilters::filterNoDescriptionPattern.name,
      TranslationFilters::filterNamespacePattern.name,
      TranslationFilters::filterNoNamespacePattern.name,
      TranslationFilters::filterTranslationPattern.name,
      TranslationFilters::filterNoTranslationPattern.name,
    )

  fun customize(binder: WebDataBinder) {
    verbatimListParams.forEach { param ->
      binder.registerCustomEditor(
        List::class.java,
        param,
        CustomCollectionEditor(List::class.java),
      )
    }
  }
}
