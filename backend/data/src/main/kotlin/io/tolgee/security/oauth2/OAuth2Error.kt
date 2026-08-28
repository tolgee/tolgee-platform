package io.tolgee.security.oauth2

/** An RFC 6749 error: [error] is the wire value (`invalid_grant`, ...), [statusCode] the HTTP status the token endpoint answers with. */
class OAuth2Error(
  val error: String,
  val description: String? = null,
  val statusCode: Int = 400,
) : RuntimeException(description?.let { "$error: $it" } ?: error) {
  companion object {
    const val INVALID_REQUEST = "invalid_request"
    const val INVALID_CLIENT = "invalid_client"
    const val INVALID_GRANT = "invalid_grant"
    const val INVALID_SCOPE = "invalid_scope"
    const val ACCESS_DENIED = "access_denied"
    const val UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type"
    const val UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type"
  }
}
