package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema
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
      "Include only keys whose translation into the task language was never part of any task. " +
        "Evaluated per task language, so a key already tasked for one language is still included " +
        "for the others.",
  )
  var filterNeverInTask: Boolean? = false,
  @Schema(
    description =
      "Include only keys whose translation into the task language is or was part of some task. " +
        "Evaluated per task language.",
  )
  var filterHasBeenInTask: Boolean? = false,
  @Schema(
    description =
      "Include only keys whose translation into the task language is not part of an open task. " +
        "A task counts as open while it is in the NEW or IN_PROGRESS state.",
  )
  var filterNotInOpenTask: Boolean? = false,
  @Schema(
    description =
      "Restrict filterNeverInTask, filterHasBeenInTask and filterNotInOpenTask to tasks of these " +
        "types. Omitted or empty means every type counts.",
  )
  var filterTaskType: List<TaskType>? = listOf(),
) {
  val filterStateOrdinal: List<Int>? get() {
    return filterState?.map { it.ordinal }
  }

  /** Postgres rejects an empty `in (...)`, so an absent filter widens to every type. */
  val matchedTaskTypeNames: List<String> get() {
    return (filterTaskType?.takeIf { it.isNotEmpty() } ?: TaskType.entries).map { it.name }
  }

  fun excludesOpenTasksOfType(type: TaskType): Boolean {
    if (filterNotInOpenTask != true) return false
    return matchedTaskTypeNames.contains(type.name)
  }
}
