package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserMfaRecoveryRequestDto(
  @field:NotBlank
  @field:Size(min = 6, max = 6)
  var otp: String = "",
)
