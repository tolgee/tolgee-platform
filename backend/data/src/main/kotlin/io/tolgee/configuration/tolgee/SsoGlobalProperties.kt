package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.sso")
@DocProperty(
  description =
    "Single sign-on (SSO) is an authentication process that allows a user to" +
      " access multiple applications with one set of login credentials. To use SSO" +
      " in Tolgee, can either configure global SSO settings in this section or" +
      " just enable SSO and configure separately for each organization in the" +
      " organization settings.",
  displayName = "Single Sign-On",
)
class SsoGlobalProperties {
  @DocProperty(description = "Enables SSO authentication")
  var enabled: Boolean = false

  val globalEnabled: Boolean
    get() = enabled && !domain.isNullOrEmpty()

  @DocProperty(description = "Unique identifier for an application")
  var clientId: String? = null

  @DocProperty(description = "Key used to authenticate the application")
  var clientSecret: String? = null

  @DocProperty(description = "URL to redirect users for authentication")
  var authorizationUri: String? = null

  @DocProperty(description = "URL for exchanging authorization code for tokens")
  var tokenUri: String? = null

  @DocProperty(description = "Used to identify the organization on login page")
  var domain: String? = null

  @DocProperty(description = "URL to retrieve the JSON Web Key Set (JWKS)")
  var jwkSetUri: String? = null

  @DocProperty(
    description =
      "Custom logo URL to be displayed on the login screen. Can be set only when `nativeEnabled` is `false`. " +
        "You may need that when you want to enable login via your custom SSO (the default logo is sso_login.svg," +
        " which is stored in the webapp/public directory).",
  )
  var customLogoUrl: String? = null

  @DocProperty(
    description = "Custom text for the SSO login page. Can be set only when `nativeEnabled` is `false`.",
  )
  var customLoginText: String? = null
}
