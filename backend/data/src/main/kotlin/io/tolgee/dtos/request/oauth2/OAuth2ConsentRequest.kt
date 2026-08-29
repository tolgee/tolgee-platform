package io.tolgee.dtos.request.oauth2

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** The user's decision on the consent screen. No approved scopes means they denied. */
data class OAuth2ConsentRequest(
  @field:NotBlank
  @field:Size(max = 255)
  @Schema(description = "The consent state identifying the pending authorization")
  val state: String,
  val scopes: List<String>? = null,
  @Schema(
    description =
      "Whether the token is bound to one project or to every project the user can reach. Required when approving: " +
        "the widest grant must be asked for, never fallen into. Ignored on a denial, which grants nothing.",
  )
  val projectScope: ProjectScope? = null,
  @Schema(description = "Required when projectScope is SINGLE_PROJECT")
  val projectId: Long? = null,
) {
  enum class ProjectScope {
    SINGLE_PROJECT,
    ALL_PROJECTS,
  }
}
