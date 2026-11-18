package io.tolgee.api.v2.controllers.translation

import io.tolgee.dtos.request.translation.TranslationFilters
import org.springframework.beans.propertyeditors.CustomCollectionEditor
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.InitBinder

/**
 * Spring splits a single `?param=a,b` query value into `["a", "b"]` for `List` fields by
 * default. The [TranslationFilters] pattern params carry free text where commas are
 * structural (`languageTag,pattern`), so they must bind verbatim.
 *
 * Scoped by the binder's target type, not by model attribute name — Spring derives a
 * different default attribute name for every [TranslationFilters] subclass, so name-keyed
 * registration silently misses future endpoints. With constructor data binding the binder
 * initializes before the target instance exists, so `targetType` must be checked too.
 *
 * `trashed` is set by the trash endpoints only. Binding it from a request would turn every endpoint
 * that takes these filters into a trash listing, also for callers the trash is closed to.
 */
@ControllerAdvice
class TranslationFiltersBindingCustomizer {
  @InitBinder
  fun customizeBinding(binder: WebDataBinder) {
    if (!bindsTranslationFilters(binder)) {
      return
    }
    binder.setDisallowedFields(TranslationFilters::trashed.name)
    TranslationFilters.VERBATIM_LIST_PARAMS.forEach { param ->
      binder.registerCustomEditor(
        List::class.java,
        param,
        CustomCollectionEditor(List::class.java),
      )
    }
  }

  private fun bindsTranslationFilters(binder: WebDataBinder): Boolean {
    if (binder.target is TranslationFilters) return true
    val targetClass = binder.targetType?.resolve() ?: return false
    return TranslationFilters::class.java.isAssignableFrom(targetClass)
  }
}
