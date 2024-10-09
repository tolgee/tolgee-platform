package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class UpdateTaskRequest(
  @field:Size(min = 3, max = 255)
  var name: String = "",
  @field:Size(min = 0, max = 2000)
  var description: String = "",
  @Schema(
    description = "Due to date in epoch format (milliseconds).",
    example = "1661172869000",
  )
  var dueDate: Long? = null,
  var assignees: MutableSet<Long> = mutableSetOf(),
)
