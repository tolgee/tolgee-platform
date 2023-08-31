package io.tolgee.dtos.security

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

class LoginRequest {
  @field:NotBlank
  var username: String = ""
  @field:NotEmpty
  var password: String = ""
  var otp: String? = null
}
