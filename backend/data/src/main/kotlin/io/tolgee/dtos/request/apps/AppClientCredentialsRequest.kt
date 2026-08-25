package io.tolgee.dtos.request.apps

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/** Client-credentials token request (JSON-encoded), exchanging app credentials + install_id for a token. */
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
  /** Nullable only so its absence yields a specific error; credentials alone cannot imply an install. */
  @JsonProperty("install_id")
  val installId: Long? = null,
)
