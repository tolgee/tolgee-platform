package io.tolgee.dtos.request.apps

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/** App-level credentials plus the secret to revoke, for the self-service revoke call. */
data class AppSecretRevokeRequest(
  @JsonProperty("client_id")
  @field:NotBlank
  val clientId: String = "",
  @JsonProperty("client_secret")
  @field:NotBlank
  val clientSecret: String = "",
  /** Nullable only so its absence produces a specific error rather than a generic validation failure. */
  @JsonProperty("secret_id")
  val secretId: Long? = null,
)
