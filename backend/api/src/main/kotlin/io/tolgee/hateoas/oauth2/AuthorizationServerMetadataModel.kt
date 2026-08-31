package io.tolgee.hateoas.oauth2

import com.fasterxml.jackson.annotation.JsonProperty

/** RFC 8414 authorization server metadata. */
data class AuthorizationServerMetadataModel(
  @get:JsonProperty("issuer")
  val issuer: String,
  @get:JsonProperty("authorization_endpoint")
  val authorizationEndpoint: String,
  @get:JsonProperty("token_endpoint")
  val tokenEndpoint: String,
  @get:JsonProperty("response_types_supported")
  val responseTypesSupported: List<String>,
  @get:JsonProperty("grant_types_supported")
  val grantTypesSupported: List<String>,
  @get:JsonProperty("code_challenge_methods_supported")
  val codeChallengeMethodsSupported: List<String>,
  @get:JsonProperty("token_endpoint_auth_methods_supported")
  val tokenEndpointAuthMethodsSupported: List<String>,
  @get:JsonProperty("scopes_supported")
  val scopesSupported: List<String>,
  /** RFC 9207 §3: tells a client the `iss` parameter will be on every authorization response. */
  @get:JsonProperty("authorization_response_iss_parameter_supported")
  val authorizationResponseIssParameterSupported: Boolean,
  @get:JsonProperty("revocation_endpoint")
  val revocationEndpoint: String,
  /** RFC 8414 §2: omitting this would default the endpoint to `client_secret_basic`, and every client here is public. */
  @get:JsonProperty("revocation_endpoint_auth_methods_supported")
  val revocationEndpointAuthMethodsSupported: List<String>,
)
