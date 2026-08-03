package io.tolgee.ee.data

/**
 * Providers may omit any of these fields: `id_token` and `refresh_token` are commonly
 * absent from refresh responses (e.g. Google), and `scope` is optional per RFC 6749.
 * Callers requiring a field must check for null.
 */
@Suppress("PropertyName")
class OAuth2TokenResponse(
  val id_token: String?,
  val scope: String?,
  val refresh_token: String?,
)
