package io.tolgee.dtos.request.auth

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class ResetPassword(
  @field:NotBlank
  var email: String? = null,

  @field:NotBlank
  var code: String? = null,

  @field:Size(min = 8, max = 100)
  var password: String? = null,
)
