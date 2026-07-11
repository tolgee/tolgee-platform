package io.tolgee.ee.data.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

class UpdateProjectTranslationMemoryAssignmentRequest {
  @Schema(description = "Whether this project can read from the TM")
  var readAccess: Boolean = true

  @Schema(description = "Whether this project writes new translations to the TM")
  var writeAccess: Boolean = true

  @Schema(
    description =
      "Priority in suggestion results (lower = higher priority). Omit to leave the current " +
        "priority unchanged.",
  )
  @field:Min(0)
  var priority: Int? = null

  @Schema(
    description =
      "Per-assignment penalty override (0–100). When null, the TM's default penalty applies.",
  )
  @field:Min(0)
  @field:Max(100)
  var penalty: Int? = null
}
