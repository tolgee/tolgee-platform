package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.Parameter

open class ProjectFilters {
  @field:Parameter(
    description = """Filter projects by id""",
  )
  var filterId: List<Long>? = null

  @field:Parameter(
    description = """Filter projects without id""",
  )
  var filterNotId: List<Long>? = null
}
