package io.tolgee.model.enums

enum class UserSessionType {
  LOGIN_NATIVE,
  LOGIN_GITHUB,
  LOGIN_GOOGLE,
  LOGIN_OAUTH2,
  LOGIN_SSO,
  SIGN_UP,
  EMAIL_VERIFICATION,
  IMPERSONATION,
  TEST,

  /**
   * Session backfilled for a token that predates session tracking, so its origin is unrecoverable.
   */
  UNKNOWN,
}
