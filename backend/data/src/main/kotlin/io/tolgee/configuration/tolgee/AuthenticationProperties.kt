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
      name = "user-can-create-projects",
      description = "Whether regular users are allowed to create projects. When disabled, only administrators can create projects",
      removedIn = "2.33.0",
      defaultValue = "true"
    )
  ],
)
class AuthenticationProperties(
  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether authentication is enabled. If not, Tolgee will create implicit user on first startup and will automatically log " +
      "you in. No login page shows, no permissions are managed. This is very useful, when you want to use Tolgee on your local " +
      "machine, or you just want to test it."
  )
  var enabled: Boolean = true,
  @DocProperty(
    description = "Secret used to sign JWT authentication tokens with. It will be generated" +
      " automatically, if not provided. You will be fine with 64 characters long random string.",
    defaultExplanation = "Generated automatically, if not provided. If running multiple replicas, it's required to" +
      " set it or otherwise you will be constantly logged out."
  )
  var jwtSecret: String? = null,
  @DocProperty(
    description = "Expiration time of generated JWT tokens in milliseconds.",
    defaultExplanation = "= 7 days"
  )
  var jwtExpiration: Int = 604800000,
  var jwtSuperExpiration: Int = 60 * 60 * 1000, // one hour,
  var nativeEnabled: Boolean = true,
  @E2eRuntimeMutable
  var registrationsAllowed: Boolean = false,
  @E2eRuntimeMutable
  var needsEmailVerification: Boolean = false,
  var createInitialUser: Boolean = true,
  var initialUsername: String = "admin",
  var initialPassword: String? = null,
  var securedImageRetrieval: Boolean = false,
  var securedImageTimestampMaxAge: Long = 600000, // one week
  var github: GithubAuthenticationProperties = GithubAuthenticationProperties(),
  var google: GoogleAuthenticationProperties = GoogleAuthenticationProperties(),
  var oauth2: OAuth2AuthenticationProperties = OAuth2AuthenticationProperties(),
  var ldap: LdapAuthenticationProperties = LdapAuthenticationProperties(),
  @E2eRuntimeMutable
  var userCanCreateOrganizations: Boolean = true
) {
  fun checkAllowedRegistrations() {
    if (!this.registrationsAllowed) {
      throw BadRequestException(io.tolgee.constants.Message.REGISTRATIONS_NOT_ALLOWED)
    }
  }
}
