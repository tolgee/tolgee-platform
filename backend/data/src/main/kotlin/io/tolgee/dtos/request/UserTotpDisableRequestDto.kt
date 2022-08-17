package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserTotpDisableRequestDto(
  @field:NotBlank
  @field:Size(min = 6, max = 36)
  var otp: String = ""
)
