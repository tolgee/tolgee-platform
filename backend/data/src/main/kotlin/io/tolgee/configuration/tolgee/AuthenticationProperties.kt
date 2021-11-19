/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.exceptions.BadRequestException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "tolgee.authentication")
class AuthenticationProperties(
  @E2eRuntimeMutable
  var enabled: Boolean = true,
  var jwtSecret: String? = null,
  var jwtExpiration: Int = 604800000,
  var nativeEnabled: Boolean = true,
  var registrationsAllowed: Boolean = false,
  @E2eRuntimeMutable
  var needsEmailVerification: Boolean = false,
  var createInitialUser: Boolean = true,
  var initialUsername: String = "admin",
  var initialPassword: String? = null,
  var securedImageRetrieval: Boolean = false,
  var securedImageTimestampMaxAge: Long = 600000, // one week
  var github: GithubAuthenticationProperties = GithubAuthenticationProperties(),
  var ldap: LdapAuthenticationProperties = LdapAuthenticationProperties(),
  var userCanCreateProjects: Boolean = true,
  var userCanCreateOrganizations: Boolean = true

) {
  fun checkAllowedRegistrations() {
    if (!this.registrationsAllowed) {
      throw BadRequestException(io.tolgee.constants.Message.REGISTRATIONS_NOT_ALLOWED)
    }
  }
}
