package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema
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
) {
  val filterStateOrdinal: List<Int>? get() {
    return filterState?.map { it.ordinal }
  }
}
