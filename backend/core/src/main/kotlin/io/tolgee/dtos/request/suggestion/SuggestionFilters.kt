package io.tolgee.dtos.request.suggestion

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.model.enums.TranslationSuggestionState

open class SuggestionFilters {
  @field:Parameter(
    description = """Filter by suggestion state""",
  )
  var filterState: List<TranslationSuggestionState>? = null
}
