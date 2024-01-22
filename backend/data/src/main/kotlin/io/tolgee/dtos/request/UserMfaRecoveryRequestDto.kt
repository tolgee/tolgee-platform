package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserMfaRecoveryRequestDto(
  @field:NotBlank
  @field:Size(max = 50)
  var password: String = "",
)
