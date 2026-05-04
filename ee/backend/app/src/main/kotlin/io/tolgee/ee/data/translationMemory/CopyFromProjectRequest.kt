package io.tolgee.ee.data.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

class CopyFromProjectRequest {
  @Schema(description = "ID of the project whose translation memory entries should be copied")
  @field:NotNull
  var sourceProjectId: Long = 0
}
