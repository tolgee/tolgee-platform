package io.tolgee.dtos.request.apps

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/**
 * OAuth 2.0 client-credentials token request (RFC 6749 §4.4). An app's backend exchanges its
 * app-level `client_id` + `client_secret` plus an `install_id` for a short-lived install-scoped
 * access token.
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
  /**
   * Which installation the token should act as. The credentials identify an app installed by any
   * number of organizations, so they cannot imply one on their own. Nullable only so its absence
   * produces a specific error rather than a generic validation failure.
   */
  @JsonProperty("install_id")
  val installId: Long? = null,
)
