package io.tolgee.ee.data.task

import io.tolgee.model.enums.TaskType
import jakarta.validation.constraints.NotNull

data class CalculateScopeRequest(
  @field:NotNull
  var languageId: Long,
  @field:NotNull
  var type: TaskType,
  @field:NotNull
  var keys: MutableSet<Long> = mutableSetOf(),
)
