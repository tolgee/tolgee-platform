package io.tolgee.hateoas.oauth2

import com.fasterxml.jackson.annotation.JsonProperty

data class ProtectedResourceMetadataModel(
  @get:JsonProperty("resource")
  val resource: String,
  @get:JsonProperty("authorization_servers")
  val authorizationServers: List<String>,
  @get:JsonProperty("scopes_supported")
  val scopesSupported: List<String>,
  @get:JsonProperty("bearer_methods_supported")
  val bearerMethodsSupported: List<String>,
)
