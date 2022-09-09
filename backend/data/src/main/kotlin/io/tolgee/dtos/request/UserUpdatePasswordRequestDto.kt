package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUpdatePasswordRequestDto(
  @field:Size(max = 50)
  var currentPassword: String = "",

  @field:Size(min = 8, max = 50)
  var password: String = ""
)
