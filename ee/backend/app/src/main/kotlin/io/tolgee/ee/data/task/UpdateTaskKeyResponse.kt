package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateTaskKeyResponse(
  @Schema(
    description = "Task key is marked as done",
  )
  val done: Boolean,
  @Schema(
    description = "Task progress is 100%",
  )
  val taskFinished: Boolean,
)
