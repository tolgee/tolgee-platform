package io.tolgee.ee.data.task

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.TaskType
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateTaskRequest(
  @field:NotBlank
  @field:Size(min = 3, max = 255)
  var name: String = "",
  @field:Size(min = 0, max = 2000)
  var description: String = "",
  @Enumerated(EnumType.STRING)
  val type: TaskType,
  @Schema(
    description = "Due to date in epoch format (milliseconds).",
    example = "1661172869000",
  )
  var dueDate: Long? = null,
  @Schema(
    description = "Id of language, this task is attached to.",
    example = "1",
  )
  @field:NotNull
  var languageId: Long? = null,
  @field:NotNull
  var assignees: MutableSet<Long>? = null,
  @field:NotNull
  var keys: MutableSet<Long>? = null,
)
