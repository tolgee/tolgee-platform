package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserTotpEnableRequestDto(
  @field:NotBlank
  @field:Size(min = 16, max = 16)
  @field:Pattern(regexp = "^(?:[A-Z2-7]{8})*$")
  var totpKey: String = "",

  @field:NotBlank
  @field:Size(min = 6, max = 6)
  var otp: String = "",

  @field:NotBlank
  @field:Size(min = 8, max = 50)
  var password: String = ""
)
