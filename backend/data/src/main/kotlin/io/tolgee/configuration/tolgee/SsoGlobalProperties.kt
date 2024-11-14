package io.tolgee.configuration.tolgee

import io.tolgee.api.ISsoTenant
import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.Organization
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import kotlin.reflect.KProperty0

@ConfigurationProperties(prefix = "tolgee.authentication.sso-global")
@DocProperty(
  description =
    "Single sign-on (SSO) is an authentication process that allows a user to" +
      " access multiple applications with one set of login credentials. To use SSO" +
      " in Tolgee, can either configure global SSO settings in this section or" +
      " in refer to `sso-organizations` section for enabling the per Organization mode.\n\n" +
      "There is a significant difference between global and per organization SSO:" +
      " Global SSO can handle authentication for all server users no matter which organizations they belong to," +
      " while per organization SSO can handle authentication only for users of the organization and" +
      " such users cannot be members of any other organization. SSO users associated with per organization SSO have" +
      " no rights to create or manage organizations. Global SSO users should be invited to organizations they need to" +
      " have access to. Per organization SSO users are automatically added to the organization they belong to.",
  displayName = "Single Sign-On",
)
class SsoGlobalProperties : ISsoTenant {
  @DocProperty(description = "Enables SSO authentication on global level - as a login method for the whole server")
  var enabled: Boolean = false

  @DocProperty(description = "Unique identifier for an application")
  override var clientId: String = ""

  @DocProperty(description = "Key used to authenticate the application")
  override var clientSecret: String = ""

  @DocProperty(description = "URL to redirect users for authentication")
  override var authorizationUri: String = ""

  @DocProperty(description = "URL for exchanging authorization code for tokens")
  override var tokenUri: String = ""

  @DocProperty(description = "Used to identify the organization on login page")
  override var domain: String = ""

  @DocProperty(description = "URL to retrieve the JSON Web Token Set (JWTS)")
  override var jwtSetUri: String = ""

  @DocProperty(
    description =
      "Minutes after which the server will recheck the user's with the SSO provider to" +
        " ensure the user account is still valid. This is to prevent the user from being" +
        " able to access the server after the account has been disabled or deleted in the SSO provider.",
  )
  var sessionExpirationMinutes: Int = 10

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

  @DocProperty(hidden = true)
  override val name: String
    get() = "Global SSO"

  @DocProperty(hidden = true)
  override val global: Boolean
    get() = true

  @DocProperty(hidden = true)
  override val organization: Organization? = null

  @PostConstruct
  fun validate() {
    if (enabled) {
      listOf(::clientId, ::clientSecret, ::authorizationUri, ::domain, this::jwtSetUri, ::tokenUri).forEach {
        it.validateIsNotBlank()
      }
    }
  }

  private fun <T : String?> KProperty0<T?>.validateIsNotBlank() =
    require(!this.get().isNullOrBlank()) { "Property ${this.name} must be set when SSO is enabled" }
}
