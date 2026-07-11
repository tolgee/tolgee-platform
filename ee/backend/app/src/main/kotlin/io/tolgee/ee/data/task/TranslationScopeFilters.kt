package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.TranslationState

data class TranslationScopeFilters(
  @Schema(
    description = "Include keys with translation in certain states",
  )
  var filterState: List<TranslationState>? = listOf(),
  @Schema(
    description = "Include keys where translation is outdated",
  )
  var filterOutdated: Boolean? = false,
) {
  val filterStateOrdinal: List<Int>? get() {
    return filterState?.map { it.ordinal }
  }
}
