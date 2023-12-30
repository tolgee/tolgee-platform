package io.tolgee.dtos.request

import io.swagger.v3.oas.annotations.media.Schema

data class SuperTokenRequest(
  @Schema(description = "Has to be provided when TOTP enabled")
  var otp: String? = null,
  @Schema(description = "Has to be provided when TOTP not enabled")
  var password: String? = null,
)
