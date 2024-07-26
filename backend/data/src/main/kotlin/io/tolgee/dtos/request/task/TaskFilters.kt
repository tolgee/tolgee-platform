package io.tolgee.dtos.request.task

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType

open class TaskFilters {
  @field:Parameter(
    description = """Filter tasks by state""",
  )
  var filterState: List<TaskState>? = null

  @field:Parameter(
    description = """Filter tasks without state""",
  )
  var filterNotState: List<TaskState>? = null

  @field:Parameter(
    description = """Filter tasks by assignee""",
  )
  var filterAssignee: List<Long>? = null

  @field:Parameter(
    description = """Filter tasks by type""",
  )
  var filterType: List<TaskType>? = null

  @field:Parameter(
    description = """Filter tasks by id""",
  )
  var filterId: List<Long>? = null

  @field:Parameter(
    description = """Filter tasks without id""",
  )
  var filterNotId: List<Long>? = null

  @field:Parameter(
    description = """Filter tasks by project""",
  )
  var filterProject: List<Long>? = null

  @field:Parameter(
    description = """Filter tasks without project""",
  )
  var filterNotProject: List<Long>? = null

  @field:Parameter(
    description = """Filter tasks by language""",
  )
  var filterLanguage: List<Long>? = null
}
