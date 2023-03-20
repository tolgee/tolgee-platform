/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.exceptions.BadRequestException
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication")
class AuthenticationProperties(
  @E2eRuntimeMutable
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
