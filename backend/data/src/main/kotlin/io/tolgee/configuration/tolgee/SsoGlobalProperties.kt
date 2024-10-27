package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.sso")
@DocProperty(
  description =
    "Single sign-on (SSO) is an authentication process that allows a user to" +
      " access multiple applications with one set of login credentials.",
  displayName = "Single Sign-On",
)
class SsoGlobalProperties {
  var enabled: Boolean = false

  @DocProperty(description = "SSO Client ID")
  var clientId: String? = null

  @DocProperty(description = "SSO Client secret")
  var clientSecret: String? = null

  @DocProperty(description = "URL to SSO authorize API endpoint. This endpoint will be exposed to the frontend.")
  var authorizationUrl: String? = null

  @DocProperty(description = "URL to SSO token API endpoint.")
  var tokenUrl: String? = null

  var domain: String? = null

  var redirectUriBase: String? = null

  var jwkSetUri: String? = null

  @DocProperty(
    description =
      "Custom logo URL to be displayed on the login screen. Can be set only when `nativeEnabled` is `false`" +
        "You may need that when you want to enable login via your custom SSO (the default logo is sso_login.svg," +
        " which is stored in the webapp/public directory).",
  )
  var customLogoUrl: String? = null

  @DocProperty(
    description = "Custom text for the login button.",
    defaultExplanation = "Defaults to 'SSO Login' if not set.",
  )
  var customButtonText: String? = null
}
