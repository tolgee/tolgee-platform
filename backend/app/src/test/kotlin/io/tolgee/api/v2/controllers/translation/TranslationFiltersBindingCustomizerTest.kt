package io.tolgee.api.v2.controllers.translation

import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.web.bind.WebDataBinder

class TranslationFiltersBindingCustomizerTest {
  private val customizer = TranslationFiltersBindingCustomizer()

  @Test
  fun `registers verbatim editors for a TranslationFilters binder`() {
    val binder = WebDataBinder(TranslationFilters(), "translationFilters")

    customizer.customizeBinding(binder)

    // without the editor Spring would comma-split the value into a List
    TranslationFilters.VERBATIM_LIST_PARAMS.forEach { param ->
      (binder.findCustomEditor(List::class.java, param) != null).assert.isTrue()
    }
  }

  @Test
  fun `ignores a binder whose target is not TranslationFilters`() {
    val binder = WebDataBinder("not a filter", "somethingElse")

    customizer.customizeBinding(binder)

    (binder.findCustomEditor(List::class.java, "filterKeyPattern") == null)
      .assert
      .isTrue()
  }
}
