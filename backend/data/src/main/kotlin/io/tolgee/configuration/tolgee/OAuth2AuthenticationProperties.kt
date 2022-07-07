package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.oauth2")
class OAuth2AuthenticationProperties {
  var clientSecret: String? = null
  var clientId: String? = null
  val scopes: MutableList<String> = mutableListOf()
  var authorizationUrl: String? = null
  var tokenUrl: String? = null
  var userUrl: String? = null
}
