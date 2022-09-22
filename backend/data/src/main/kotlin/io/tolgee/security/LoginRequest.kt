package io.tolgee.security

import javax.validation.constraints.NotBlank

class LoginRequest {
  @NotBlank
  var username: String = ""
  @NotBlank
  var password: String = ""
  var otp: String? = null
}
