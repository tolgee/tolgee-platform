package io.tolgee.dtos.request.task

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.tolgee.model.enums.TaskState

open class TaskFilters {
  @field:Parameter(
    description = """Filter tasks with the state""",
  )
  var filterState: List<TaskState>? = null

  @field:Parameter(
    description = """Filter tasks without the state""",
  )
  var filterNotState: List<TaskState>? = null
}
