/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.exceptions.BadRequestException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "tolgee.authentication")
class AuthenticationProperties(
  @E2eRuntimeMutable
  @Schema(
    description = "Whether authentication is enabled. If not, Tolgee will create implicit user on first startup and will automatically log\n" +
      "you in. No login page shows, no permissions are managed. This is very useful, when you want to use Tolgee on your local\n" +
      "machine, or you just want to test it."
  )
  var enabled: Boolean = true,
  var jwtSecret: String? = null,
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
