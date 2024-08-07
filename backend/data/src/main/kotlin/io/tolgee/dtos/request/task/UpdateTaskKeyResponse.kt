package io.tolgee.dtos.request.task

import jakarta.validation.constraints.NotNull

data class UpdateTaskKeyResponse(
  val done: Boolean,
  val taskFinished: Boolean,
)
