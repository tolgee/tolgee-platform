/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.AdditionalDocsProperties
import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.exceptions.BadRequestException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import javax.validation.constraints.Size

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
    ),
    DocProperty(
      name = "createInitialUser",
      description = "If true, Tolgee creates initial user on first start-up.",
      removedIn = "3.31.0",
      removalReason = "Presence of this initial account is now expected for Tolgee to operate as expected.\n" +
        "For instance, when authentication is disabled, users are automatically logged in as this admin user.",
      defaultValue = "true",
    ),
    DocProperty(
      name = "ldap.*",
      description = "LDAP-related settings.",
      removedIn = "3.31.0",
      removalReason = "LDAP is no longer supported due to unstable and unmaintained implementation.",
    ),
  ],
)
class AuthenticationProperties(
  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether authentication is enabled." +
      "When authentication is disabled, there are no login screen and no permission control." +
      "Users get automatically logged in as the administrator account of the Tolgee instance." +
      "This is very useful, when you want to use Tolgee on your local machine, or you just want to test it.",
    defaultExplanation = "when running with Java directly, `false` when running via Docker."
  )
  var enabled: Boolean = true,

  @DocProperty(
    description = "Secret used to sign JWT authentication tokens with. It will be generated" +
      " automatically, if not provided. You will be fine with 64 characters long random string.\n\n" +
      ":::warning\n" +
      "If running multiple replicas, it's required to set it or otherwise you will be constantly logged out.\n" +
      ":::"
  )
  @Size(min = 32)
  var jwtSecret: String? = null,

  @DocProperty(
    description = "Expiration time of generated JWT tokens in milliseconds.",
    defaultExplanation = "= 7 days"
  )
  var jwtExpiration: Long = 7 * 24 * 60 * 60 * 1000,

  @DocProperty(
    description = "Expiration time of generated JWT tokens for superuser in milliseconds.",
    defaultExplanation = "= 1 hour"
  )
  var jwtSuperExpiration: Long = 60 * 60 * 1000,

  @DocProperty(
    description = "Whether to enable Tolgee-native authentication and registration.\n" +
      "When set to `false`, users will only be able to register and login via third-party SSO options (e.g. OAuth)."
  )
  var nativeEnabled: Boolean = true,

  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether users are allowed to register on Tolgee.\n" +
      "When set to `false`, existing users must send invites to projects to new users for them to be able to register."
  )
  var registrationsAllowed: Boolean = false,

  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether users need to verify their email addresses when creating their account. " +
      "Requires a valid [SMTP configuration](#SMTP)."
  )
  var needsEmailVerification: Boolean = false,

  @DocProperty(
    description = "Username of initial user.\n" +
      ":::tip" +
      "Tolgee will ask for an email instead of a username - don't worry, just give it the username specified here." +
      ":::"
  )
  var initialUsername: String = "admin",

  @DocProperty(
    description = "Password of initial user. If unspecified, a random password will be generated " +
      "and stored in the `initial.pwd` file, located at the root of Tolgee's data path."
  )
  var initialPassword: String? = null,

  @DocProperty(
    description = "Whether image assets should be protected by Tolgee. " +
      "When enabled, all images are served with an access token valid for\n" +
      "a set period of time to prevent unauthorized access to images."
  )
  var securedImageRetrieval: Boolean = false,

  @DocProperty(
    description = "Expiration time of a generated image access token in milliseconds.",
    defaultExplanation = "= 10 minutes"
  )
  var securedImageTimestampMaxAge: Long = 10 * 60 * 1000,

  @E2eRuntimeMutable
  @DocProperty(
    description = "Whether regular users are allowed to create organizations. " +
      "When `false`, only administrators can create organizations.\n" +
      "By default, when the user has no organization, one is created for them; " +
      "this doesn't apply when this setting is set to `false`. " +
      "In that case, the user without organization has no permissions on the server.",
  )
  var userCanCreateOrganizations: Boolean = true,

  var github: GithubAuthenticationProperties = GithubAuthenticationProperties(),
  var google: GoogleAuthenticationProperties = GoogleAuthenticationProperties(),
  var oauth2: OAuth2AuthenticationProperties = OAuth2AuthenticationProperties(),
) {
  fun checkAllowedRegistrations() {
    if (!this.registrationsAllowed) {
      throw BadRequestException(io.tolgee.constants.Message.REGISTRATIONS_NOT_ALLOWED)
    }
  }
}
