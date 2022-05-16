package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.google")
class GoogleAuthenticationProperties {
  var clientSecret: String? = null
  var clientId: String? = null
  var workspaceDomain: String? = null
  var authorizationUrl: String = "https://oauth2.googleapis.com/token"
  var userUrl: String = "https://www.googleapis.com/oauth2/v3/userinfo"
}
