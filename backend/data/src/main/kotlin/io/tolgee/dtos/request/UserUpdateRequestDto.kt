package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUpdateRequestDto(
  @field:NotBlank
  var name: String = "",

  @field:NotBlank
  @field:Email
  var email: String = "",

  @field:Size(min = 8, max = 100)
  var password: String? = null,

  @Schema(
    description = "Callback url for link sent in e-mail." +
      " This may be omitted, when server has set frontEndUrl in properties."
  )
  var callbackUrl: String? = null
)
