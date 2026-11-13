package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUpdatePasswordRequestDto(
  @field:NotBlank
  var currentPassword: String = "",
  @field:Size(min = 8, max = 50)
  var password: String = "",
)
