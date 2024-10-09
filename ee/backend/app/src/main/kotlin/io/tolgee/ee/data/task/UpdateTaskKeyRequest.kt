package io.tolgee.ee.data.task

import jakarta.validation.constraints.NotNull

data class UpdateTaskKeyRequest(
  @field:NotNull
  var done: Boolean,
)
