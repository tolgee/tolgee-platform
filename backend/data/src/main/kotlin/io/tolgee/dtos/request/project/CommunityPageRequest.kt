package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CommunityPageRequest(
  @Schema(description = "Markdown shown on the community page. Empty or missing clears it.")
  @field:Size(max = 10000)
  var communityDescription: String? = null,
  @Schema(
    description = "Markdown on how to contribute. Empty or missing clears it, and the page shows its default text.",
  )
  @field:Size(max = 10000)
  var communityGuidelines: String? = null,
  @Schema(description = "Whether the owner marked the community page as shared outside Tolgee.")
  var communityShared: Boolean = false,
  @Schema(description = "Whether the community page tells visitors that the app supports in-context translation.")
  var communityInContext: Boolean = false,
  @Schema(description = "Link to the app that supports in-context translation. Only http and https are accepted.")
  @field:Size(max = 255)
  @field:Pattern(regexp = "^\\s*(https?://\\S+)?\\s*$", message = "invalid_pattern")
  var communityAppUrl: String? = null,
)
