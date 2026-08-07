package io.tolgee.dtos.request.apps

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AppSelfRegisterRequest(
  @field:NotBlank
  @field:Size(max = 255)
  val manifestUrl: String = "",
  @field:Size(max = 255)
  @Schema(
    description =
      "Slug of the organization owning the app. Leave empty to register a native (server-level) " +
        "app, which belongs to no organization and is made available to organizations by a server admin.",
  )
  val organizationSlug: String? = null,
)
