package io.tolgee.dtos.request.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class ResetPasswordRequest(
  @field:NotBlank
  var callbackUrl: String? = null,
  @field:Email
  @field:NotBlank
  var email: String,
)
