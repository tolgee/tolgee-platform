package io.tolgee.dtos.request.oauth2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** The client's `/oauth2/authorize` query, as the consent screen hands it back for the authorization to be opened. */
data class OAuth2AuthorizeRequest(
  @JsonProperty("client_id")
  @field:NotBlank
  @field:Size(max = 255)
  @Schema(description = "Registered client id from the client's authorize request")
  val clientId: String,
  @JsonProperty("redirect_uri")
  @field:NotBlank
  @field:Size(max = 2000)
  @Schema(description = "Redirect URI from the client's authorize request; must be registered for the client")
  val redirectUri: String,
  @JsonProperty("response_type")
  val responseType: String? = null,
  @field:Size(max = 4000)
  val scope: String? = null,
  @field:Size(max = 2000)
  val state: String? = null,
  @JsonProperty("code_challenge")
  @field:Size(max = 255)
  val codeChallenge: String? = null,
  @JsonProperty("code_challenge_method")
  val codeChallengeMethod: String? = null,
  val project: String? = null,
)
