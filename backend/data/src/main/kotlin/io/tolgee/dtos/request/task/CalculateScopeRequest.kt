package io.tolgee.dtos.request.task

import io.tolgee.model.enums.TaskType
import jakarta.validation.constraints.NotNull

data class CalculateScopeRequest(
  @field:NotNull
  var language: Long,
  @field:NotNull
  var type: TaskType,
  @field:NotNull
  var keys: MutableSet<Long>? = null,
)
