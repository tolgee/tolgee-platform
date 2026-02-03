package io.tolgee.dtos.request.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPassword(
  @field:NotBlank
  var email: String? = null,
  @field:NotBlank
  var code: String? = null,
  @field:Size(min = 8, max = 50)
  var password: String? = null,
)
