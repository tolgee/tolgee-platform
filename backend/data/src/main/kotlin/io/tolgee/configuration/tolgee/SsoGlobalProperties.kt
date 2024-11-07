package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

// TODO: Should we allow "Global SSO" users to manage/create their own organizations?
// Basically the global SSO would become a separate login method similar to other oAuth providers.
@ConfigurationProperties(prefix = "tolgee.authentication.sso")
@DocProperty(
  description =
    "Single sign-on (SSO) is an authentication process that allows a user to" +
      " access multiple applications with one set of login credentials. To use SSO" +
      " in Tolgee, can either configure global SSO settings in this section or" +
      " just set the `enable` to `true` and configure it separately for each organization in the" +
      " organization settings.\n\n" +
      "There is a significant difference between global and per organization SSO:" +
      " Global SSO can handle authentication for all server users no matter which organizations they belong to," +
      " while per organization SSO can handle authentication only for users of the organization and" +
      " such users cannot be members of any other organization. In both cases though, the SSO user has no" +
      " rights to create or manage organizations. Global SSO users has to be invited to organizations they should" +
      " have access to. Per organization SSO users are automatically added to the organization they belong to.",
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
        "You may need that when you want to enable login via your custom SSO.",
  )
  var customLogoUrl: String? = null

  @DocProperty(
    description = "Custom text for the SSO login page.",
  )
  var customLoginText: String? = null
}
