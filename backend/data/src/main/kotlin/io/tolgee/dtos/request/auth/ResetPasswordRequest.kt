package io.tolgee.dtos.request.auth

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class ResetPasswordRequest(
  @field:NotBlank
  var callbackUrl: String? = null,

  @field:Email
  @field:NotBlank
  var email: String? = null
)
