package io.tolgee.dtos.request.apps

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/**
 * App-level credentials presented on a self-service call that needs nothing else, such as install
 * discovery. They authenticate the request itself; nothing is exchanged for a session.
 */
data class AppCredentialsRequest(
  @JsonProperty("client_id")
  @field:NotBlank
  val clientId: String = "",
  @JsonProperty("client_secret")
  @field:NotBlank
  val clientSecret: String = "",
)
