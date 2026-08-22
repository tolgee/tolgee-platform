package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterAppRequest(
  @field:NotBlank
  @field:Size(max = 255)
  val manifestUrl: String = "",
  @Schema(
    description =
      "SHA-256 hex of the manifest returned by the preview. When present, the server refetches the " +
        "manifest and rejects the request with `app_manifest_changed` if its bytes differ, so an app " +
        "cannot widen the scopes it requests between the consent preview and this call.",
  )
  val manifestHash: String? = null,
  @Schema(
    description =
      "Whether to also install the app for this organization. Defaults to true; set false to only " +
        "register it (the app-level credentials are still returned).",
  )
  val install: Boolean = true,
)
