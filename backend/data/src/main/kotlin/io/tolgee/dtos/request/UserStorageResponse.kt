package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserStorageResponse(
  @field:Schema(description = "The data stored for the field")
  var data: Any? = null,
)
