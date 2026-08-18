package io.tolgee.dtos.request.apps

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AppSelfRegisterRequest(
  @field:NotBlank
  @field:Size(max = 255)
  val manifestUrl: String = "",
  @Schema(
    description =
      "Slug of the organization the app registers into, which owns it. Omitted or blank, the app " +
        "registers into the server's initial organization.",
  )
  @field:Size(max = 255)
  val organizationSlug: String? = null,
)
