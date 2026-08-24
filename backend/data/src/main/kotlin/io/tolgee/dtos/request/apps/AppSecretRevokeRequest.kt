package io.tolgee.dtos.request.apps

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

/**
 * App-level credentials plus the secret to revoke, presented on the self-service revoke call. The
 * credentials authenticate the request itself rather than being exchanged for a token: app-level
 * credentials administer the app and never reach anyone's data, and keeping them out of the token
 * machinery is what guarantees that.
 */
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
