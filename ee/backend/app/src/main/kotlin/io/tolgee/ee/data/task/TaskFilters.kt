package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.model.enums.TaskState
import io.tolgee.model.enums.TaskType
import java.util.*

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

  @field:Parameter(
    description = """Filter tasks by key""",
  )
  var filterKey: List<Long>? = null

  @field:Parameter(
    description = """Exclude "done" tasks which are older than specified timestamp""",
  )
  var filterDoneMinClosedAt: Long? = null
}
