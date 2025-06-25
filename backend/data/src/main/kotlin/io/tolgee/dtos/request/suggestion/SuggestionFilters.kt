package io.tolgee.dtos.request.suggestion

import io.swagger.v3.oas.annotations.Parameter

open class SuggestionFilters {
  @field:Parameter(
    description = """Filter by key id""",
  )
  var filterKeyId: List<Long>? = null

  @field:Parameter(
    description = """Filter by language id""",
  )
  var filterLanguageId: List<Long>? = null
}
