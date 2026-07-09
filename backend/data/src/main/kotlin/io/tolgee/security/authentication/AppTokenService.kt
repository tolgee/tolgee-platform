package io.tolgee.security.authentication

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.security.Key
import java.util.Date

/**
 * Mints and validates user-context JWTs for Tolgee Apps.
 *
 * The token is a thin pointer: it carries identity claims (installId, projectId, userId)
 * but no permissions. Permissions are resolved from the database on every request via
 * the install's `grantedScopes` and the user's project permissions, so revocation works
 * the moment any of those change.
 *
 * Reuses [JwtService]'s signing key for the PoC. A separate signing key is a planned
 * follow-up (see Tolgee Apps architecture notes).
 */
@Service
class AppTokenService(
  @Qualifier("jwt_signing_key")
  private val signingKey: Key,
  private val authenticationProperties: AuthenticationProperties,
  private val currentDateProvider: CurrentDateProvider,
) {
  private val jwtParser: JwtParser =
    Jwts
      .parserBuilder()
      .setClock { currentDateProvider.date }
      .setSigningKey(signingKey)
      .build()

  /**
   * Mints a user-context app token. The token authorizes API calls made on behalf of
   * the given user, within the given project, scoped to the given install.
   */
  fun mintUserContextToken(
    installId: Long,
    userId: Long,
    projectId: Long,
  ): String {
    val now = currentDateProvider.date
    val expiration = Date(now.time + authenticationProperties.jwtExpiration)

    return Jwts
      .builder()
      .signWith(signingKey)
      .setIssuedAt(now)
      .setAudience(JWT_APP_TOKEN_AUDIENCE)
      .setSubject(userId.toString())
      .claim(JWT_APP_TOKEN_INSTALL_ID_CLAIM, installId)
      .claim(JWT_APP_TOKEN_PROJECT_ID_CLAIM, projectId)
      .setExpiration(expiration)
      .compact()
  }

  /**
   * Parses and verifies the token. Checks signature, audience, and expiry only — the
   * existence / non-revocation of the install, user, project and per-project enablement
   * is validated by the authentication filter against current DB state on every request.
   */
  fun validateToken(token: String): AppTokenClaims {
    val jws =
      try {
        jwtParser.parseClaimsJws(token)
      } catch (ex: Exception) {
        when (ex) {
          is SignatureException,
          is MalformedJwtException,
          is UnsupportedJwtException,
          is IllegalArgumentException,
          -> throw AuthenticationException(Message.INVALID_JWT_TOKEN)
          is ExpiredJwtException -> throw AuthenticationException(Message.EXPIRED_JWT_TOKEN)
          else -> throw ex
        }
      }

    if (jws.body.audience != JWT_APP_TOKEN_AUDIENCE) {
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    val userId =
      jws.body.subject?.toLongOrNull()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    val installId =
      (jws.body[JWT_APP_TOKEN_INSTALL_ID_CLAIM] as? Number)?.toLong()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    val projectId =
      (jws.body[JWT_APP_TOKEN_PROJECT_ID_CLAIM] as? Number)?.toLong()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    return AppTokenClaims(
      installId = installId,
      userId = userId,
      projectId = projectId,
      issuedAt = jws.body.issuedAt,
    )
  }

  companion object {
    const val JWT_APP_TOKEN_AUDIENCE = "tg.app"
    const val JWT_APP_TOKEN_INSTALL_ID_CLAIM = "tg.app.inst"
    const val JWT_APP_TOKEN_PROJECT_ID_CLAIM = "tg.app.proj"
  }
}

data class AppTokenClaims(
  val installId: Long,
  val userId: Long,
  val projectId: Long,
  val issuedAt: Date,
)
