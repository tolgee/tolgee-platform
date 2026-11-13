package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserMfaRecoveryRequestDto(
  @field:NotBlank
  var password: String = "",
)
