package io.tolgee.dtos.security

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

class LoginRequest {
  @field:NotBlank
  var username: String = ""

  @field:NotEmpty
  var password: String = ""
  var otp: String? = null
}
