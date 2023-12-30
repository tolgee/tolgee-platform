package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.model.Language
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import jakarta.persistence.criteria.Selection
import kotlin.reflect.KProperty1

class QuerySelection : LinkedHashMap<String, Selection<*>>() {
  operator fun set(
    field: Pair<Language, KProperty1<TranslationView, *>>,
    value: Selection<*>,
  ) {
    this[KeyWithTranslationsView::translations.name + "." + field.first.tag + "." + field.second.name] =
      value
  }
}
