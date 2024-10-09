package io.tolgee.dtos.request.task

import io.swagger.v3.oas.annotations.Parameter

open class UserAccountFilters {
  @field:Parameter(
    description = """Filter users by id""",
  )
  var filterId: List<Long>? = null

  @field:Parameter(
    description = """Filter users without id""",
  )
  var filterNotId: List<Long>? = null
}
