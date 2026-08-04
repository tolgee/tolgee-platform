package io.tolgee.dtos.request.apps

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/**
 * OAuth 2.0 client-credentials token request (RFC 6749 §4.4). An app's backend exchanges its
 * `client_id` + `client_secret` for a short-lived install-context access token.
 */
data class AppClientCredentialsRequest(
  @JsonProperty("grant_type")
  @field:NotBlank
  val grantType: String = "",
  @JsonProperty("client_id")
  @field:NotBlank
  val clientId: String = "",
  @JsonProperty("client_secret")
  @field:NotBlank
  val clientSecret: String = "",
)
