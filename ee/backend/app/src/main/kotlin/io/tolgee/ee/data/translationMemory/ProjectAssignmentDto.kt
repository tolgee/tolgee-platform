package io.tolgee.ee.data.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class ProjectAssignmentDto {
  @Schema(description = "Project ID")
  var projectId: Long = 0

  @Schema(description = "Whether the project can read from this TM")
  var readAccess: Boolean = true

  @Schema(description = "Whether the project can write to this TM")
  var writeAccess: Boolean = true

  @Schema(
    description =
      "Per-assignment penalty override (0–100). When null, the TM's default penalty applies.",
  )
  @field:Min(0)
  @field:Max(100)
  var penalty: Int? = null
}
