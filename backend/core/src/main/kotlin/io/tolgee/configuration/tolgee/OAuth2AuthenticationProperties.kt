package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.oauth2")
@DocProperty(
  description =
    "OAuth 2.0 is the industry-standard protocol for authorization.\n" +
      "This enables the integration of a wide range of authorization providers into tolgee, " +
      "such as Auth0, KeyCloak, Okta and others.",
  displayName = "OAuth2",
)
class OAuth2AuthenticationProperties {
  @DocProperty(description = "OAuth2 Client ID")
  var clientId: String? = null

  @DocProperty(description = "OAuth2 Client secret")
  var clientSecret: String? = null

  @DocProperty(
    description =
      "Oauth2 scopes (as list)\n" +
        "Tolgee absolutely requires rights to view the email and user information (also known as openid data).\n" +
        "In most cases the scopes `openid email profile` is used for this. " +
        "(But can also be different depending on the provider)",
  )
  val scopes: MutableList<String> = mutableListOf()

  @DocProperty(description = "URL to OAuth2 authorize API endpoint. This endpoint will exposed to the frontend.")
  var authorizationUrl: String? = null

  @DocProperty(description = "URL to OAuth2 token API endpoint.")
  var tokenUrl: String? = null

  @DocProperty(description = "URL to OAuth2 userinfo API endpoint.")
  var userUrl: String? = null
}
