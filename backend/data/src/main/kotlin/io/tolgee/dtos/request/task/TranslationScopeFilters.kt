package io.tolgee.dtos.request.task

import io.tolgee.model.enums.TranslationState

open class TranslationScopeFilters {
  var filterState: List<TranslationState>? = listOf()
  var filterOutdated: Boolean? = false
  var filterNotOutdated: Boolean? = false

  val filterStateOrdinal: List<Int>? get() {
    return filterState?.map { it.ordinal }
  }
}
