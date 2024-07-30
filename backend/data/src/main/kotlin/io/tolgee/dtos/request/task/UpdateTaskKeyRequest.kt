package io.tolgee.dtos.request.task

import jakarta.validation.constraints.NotNull

data class UpdateTaskKeyRequest(
  @field:NotNull
  var done: Boolean? = null,
)
