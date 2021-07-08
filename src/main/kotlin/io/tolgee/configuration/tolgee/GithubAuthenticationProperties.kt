package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.github")
class GithubAuthenticationProperties {
  var clientSecret: String? = null
  var clientId: String? = null
  var authorizationUrl: String = "https://github.com/login/oauth/access_token"
  var userUrl: String = "https://api.github.com/user"
}
