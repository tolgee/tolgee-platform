package io.polygloat.configuration.polygloat

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "polygloat.authentication.github")
class GithubAuthenticationProperties {
    var clientSecret: String? = null
    var clientId: String? = null
    var authorizationUrl: String = "https://github.com/login/oauth/access_token"
    var userUrl: String = "https://api.github.com/user"
}
