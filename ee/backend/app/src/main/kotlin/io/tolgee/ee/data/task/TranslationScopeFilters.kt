package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.TaskFilterStatus
import io.tolgee.model.enums.TaskType
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
  @Schema(
    description =
      "Filter by task membership, as `taskType,status` — e.g. `TRANSLATE,NEVER_IN_TASK`. Status is " +
        "one of IN_OPEN_TASK, NOT_IN_OPEN_TASK, HAS_BEEN_IN_TASK, NEVER_IN_TASK; a task counts as " +
        "open while it is NEW or IN_PROGRESS. Evaluated against the task language, so a key already " +
        "tasked for one language is still included for the others. Conditions on different task " +
        "types are combined with AND.",
  )
  var filterTaskInStatus: List<String>? = listOf(),
) {
  val filterStateOrdinal: List<Int>? get() {
    return filterState?.map { it.ordinal }
  }

  /**
   * Parsed once up front: the `…Types` getters below are read from SpEL inside the native queries,
   * where a parse failure would surface as a 500 instead of a 400.
   */
  private val byStatus: Map<TaskFilterStatus, List<TaskType>> by lazy {
    TaskScopeFilterByTask
      .parseList(filterTaskInStatus.orEmpty())
      .groupBy({ it.status }, { it.taskType })
      // a repeated pair would inflate the `…Count` past what `count(distinct type)` can reach,
      // turning the positive clauses into something no key can satisfy
      .mapValues { (_, types) -> types.distinct() }
  }

  fun validate() {
    byStatus
  }

  private fun typeNames(status: TaskFilterStatus): List<String> = byStatus[status].orEmpty().map { it.name }

  /**
   * Postgres rejects an empty `in (...)`, so an unused status renders a one-element dummy list and
   * its clause is switched off by the matching `…Count` being zero.
   */
  private fun typeNamesOrDummy(status: TaskFilterStatus): List<String> = typeNames(status).ifEmpty { listOf("") }

  val inOpenTaskTypes: List<String> get() = typeNamesOrDummy(TaskFilterStatus.IN_OPEN_TASK)
  val inOpenTaskTypesCount: Int get() = typeNames(TaskFilterStatus.IN_OPEN_TASK).size
  val notInOpenTaskTypes: List<String> get() = typeNamesOrDummy(TaskFilterStatus.NOT_IN_OPEN_TASK)
  val notInOpenTaskTypesCount: Int get() = typeNames(TaskFilterStatus.NOT_IN_OPEN_TASK).size
  val hasBeenInTaskTypes: List<String> get() = typeNamesOrDummy(TaskFilterStatus.HAS_BEEN_IN_TASK)
  val hasBeenInTaskTypesCount: Int get() = typeNames(TaskFilterStatus.HAS_BEEN_IN_TASK).size
  val neverInTaskTypes: List<String> get() = typeNamesOrDummy(TaskFilterStatus.NEVER_IN_TASK)
  val neverInTaskTypesCount: Int get() = typeNames(TaskFilterStatus.NEVER_IN_TASK).size

  /**
   * True when the filters already drop every key an open task of [type] would block, which lets the
   * caller skip the second, conflict-counting query.
   */
  fun excludesOpenTasksOfType(type: TaskType): Boolean {
    val notInOpen = byStatus[TaskFilterStatus.NOT_IN_OPEN_TASK].orEmpty()
    val never = byStatus[TaskFilterStatus.NEVER_IN_TASK].orEmpty()
    return notInOpen.contains(type) || never.contains(type)
  }
}
