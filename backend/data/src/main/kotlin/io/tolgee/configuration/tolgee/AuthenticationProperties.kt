/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.AdditionalDocsProperties
import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.exceptions.BadRequestException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "tolgee.authentication")
@AdditionalDocsProperties(
  properties = [
    DocProperty(
      name = "userCanCreateProjects",
      description = "Whether regular users are allowed to create projects. " +
        "When disabled, only administrators can create projects",
      removedIn = "2.33.0",
      defaultValue = "true"
    )
  ],
)
@DocProperty(description = "Configuration of Tolgee's authentication.", displayName = "Authentication")
class AuthenticationProperties(
  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether authentication is enabled. " +
      "If not, Tolgee will create implicit user on first startup and will automatically log " +
      "you in. No login page shows, no permissions are managed. " +
      "This is very useful, when you want to use Tolgee on your local " +
      "machine, or you just want to test it."
  )
  var enabled: Boolean = true,

  @DocProperty(
    description = "Secret used to sign JWT authentication tokens with. It will be generated" +
      " automatically, if not provided. You will be fine with 64 characters long random string.\n" +
      "Generated automatically, if not provided. If running multiple replicas, it's required to" +
      " set it or otherwise you will be constantly logged out."
  )
  var jwtSecret: String? = null,

  @DocProperty(
    description = "Expiration time of generated JWT tokens in milliseconds.",
    defaultExplanation = "≈ 7 days"
  )
  var jwtExpiration: Int = 604800000,

  @DocProperty(
    description = "Expiration time of generated JWT tokens for superuser in milliseconds.",
    defaultExplanation = "≈ 1 hour"
  )
  var jwtSuperExpiration: Int = 60 * 60 * 1000,

  @DocProperty(
    description = "Whether user credentials are stored in Tolgee's database. " +
      "If you would like to use LDAP, set this to `false`."
  )
  var nativeEnabled: Boolean = true,

  @E2eRuntimeMutable
  @DocProperty(
    description = "Enable/disable sign ups into Tolgee."
  )
  var registrationsAllowed: Boolean = false,

  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether users need to verify their email addresses when creating their account. " +
      "Requires a valid [SMTP configuration](#SMTP)."
  )
  var needsEmailVerification: Boolean = false,

  @DocProperty(
    description = "If true, Tolgee creates initial user on first start-up."
  )
  var createInitialUser: Boolean = true,

  @DocProperty(
    description = "Username of initial user."
  )
  var initialUsername: String = "admin",

  @DocProperty(
    description = "Password of initial user. If unspecified, a random password will be generated " +
      "and stored in the `initial.pwd` file,\n" +
      "located at the root of Tolgee's data path."
  )
  var initialPassword: String? = null,

  @DocProperty(
    description = "Whether image assets should be protected by Tolgee. " +
      "When enabled, all images are served with a secure token valid for\n" +
      "a set period of time to prevent unauthorized access to images."
  )
  var securedImageRetrieval: Boolean = false,

  @DocProperty(
    description = "Expiration time of a generated image access token in seconds.",
    defaultExplanation = "≈ one week"
  )
  var securedImageTimestampMaxAge: Long = 600000,

  var github: GithubAuthenticationProperties = GithubAuthenticationProperties(),
  var google: GoogleAuthenticationProperties = GoogleAuthenticationProperties(),
  var oauth2: OAuth2AuthenticationProperties = OAuth2AuthenticationProperties(),
  var ldap: LdapAuthenticationProperties = LdapAuthenticationProperties(),

  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether regular users are allowed to create organizations. " +
      "When `false`, only administrators can create organizations.\n" +
      "By default, when the user has no organization, one is created for them; " +
      "this doesn't apply when this setting is set to `false`. " +
      "In that case, the user without organization has no permissions on the server.",
  )
  var userCanCreateOrganizations: Boolean = true
) {
  fun checkAllowedRegistrations() {
    if (!this.registrationsAllowed) {
      throw BadRequestException(io.tolgee.constants.Message.REGISTRATIONS_NOT_ALLOWED)
    }
  }
}
