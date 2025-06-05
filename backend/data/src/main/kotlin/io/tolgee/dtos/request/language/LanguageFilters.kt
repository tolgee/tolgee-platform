package io.tolgee.dtos.request.language

import io.swagger.v3.oas.annotations.Parameter

open class LanguageFilters {
  @field:Parameter(
    description = """Filter languages by id""",
  )
  var filterId: List<Long>? = null

  @field:Parameter(
    description = """Filter languages without id""",
  )
  var filterNotId: List<Long>? = null

  @field:Parameter(
    description = """Filter languages by name or tag""",
  )
  var search: String? = null
}
