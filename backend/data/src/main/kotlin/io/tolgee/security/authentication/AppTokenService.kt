package io.tolgee.security.authentication

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.security.Key
import java.util.Date

/**
 * Mints and validates the JWTs Tolgee Apps use to call the REST API. Two token contexts exist:
 *
 *  - **user-context** — minted for the dashboard iframe. Bound to (install, project, user); the
 *    app acts on behalf of a signed-in user and is capped to the intersection of the install's
 *    granted scopes and that user's project permissions.
 *  - **install-context** — minted for the app's backend (machine-to-machine) after it authenticates
 *    with its OAuth client credentials. Bound to the install only; the app acts with the install's
 *    full granted scopes (optionally narrowed to an acted-as project member).
 *
 * Either way the token is a thin pointer: it carries identity claims but no permissions. Permissions
 * are resolved from the database on every request, so revocation takes effect immediately.
 *
 * Signed with a dedicated app-token key ([AuthenticationConfig.appsJwtSigningKey]), domain-separated
 * from the user-session key.
 */
@Service
class AppTokenService(
  @Qualifier("apps_jwt_signing_key")
  private val signingKey: Key,
  private val appsProperties: AppsProperties,
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
    isReadOnly: Boolean,
  ): String {
    val builder =
      baseBuilder(installId)
        .setSubject(userId.toString())
        .claim(JWT_APP_TOKEN_CONTEXT_CLAIM, CONTEXT_USER)
        .claim(JWT_APP_TOKEN_PROJECT_ID_CLAIM, projectId)

    if (isReadOnly) {
      builder.claim(JWT_APP_TOKEN_READ_ONLY_CLAIM, true)
    }

    return builder.compact()
  }

  /**
   * Mints an install-context app token for the app backend (machine-to-machine). The token is bound
   * to the install only; it is not tied to a project or user.
   */
  fun mintInstallContextToken(installId: Long): String {
    return baseBuilder(installId)
      .claim(JWT_APP_TOKEN_CONTEXT_CLAIM, CONTEXT_INSTALL)
      .compact()
  }

  private fun baseBuilder(installId: Long) =
    Jwts
      .builder()
      .signWith(signingKey)
      .setIssuedAt(currentDateProvider.date)
      .setAudience(JWT_APP_TOKEN_AUDIENCE)
      .setExpiration(Date(currentDateProvider.date.time + appsProperties.tokenExpiration))
      .claim(JWT_APP_TOKEN_INSTALL_ID_CLAIM, installId)

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
          is ExpiredJwtException -> throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
          else -> throw ex
        }
      }

    if (jws.body.audience != JWT_APP_TOKEN_AUDIENCE) {
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    val installId =
      (jws.body[JWT_APP_TOKEN_INSTALL_ID_CLAIM] as? Number)?.toLong()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    val isReadOnly = jws.body[JWT_APP_TOKEN_READ_ONLY_CLAIM] as? Boolean ?: false

    if (jws.body[JWT_APP_TOKEN_CONTEXT_CLAIM] == CONTEXT_INSTALL) {
      return AppTokenClaims(
        installId = installId,
        isInstallContext = true,
        userId = null,
        projectId = null,
        issuedAt = jws.body.issuedAt,
        isReadOnly = isReadOnly,
      )
    }

    val userId =
      jws.body.subject?.toLongOrNull()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    val projectId =
      (jws.body[JWT_APP_TOKEN_PROJECT_ID_CLAIM] as? Number)?.toLong()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    return AppTokenClaims(
      installId = installId,
      isInstallContext = false,
      userId = userId,
      projectId = projectId,
      issuedAt = jws.body.issuedAt,
      isReadOnly = isReadOnly,
    )
  }

  companion object {
    const val JWT_APP_TOKEN_AUDIENCE = "tg.app"
    const val JWT_APP_TOKEN_INSTALL_ID_CLAIM = "tg.app.inst"
    const val JWT_APP_TOKEN_PROJECT_ID_CLAIM = "tg.app.proj"
    const val JWT_APP_TOKEN_CONTEXT_CLAIM = "tg.app.ctx"
    const val JWT_APP_TOKEN_READ_ONLY_CLAIM = "tg.app.ro"
    const val CONTEXT_USER = "user"
    const val CONTEXT_INSTALL = "install"
  }
}

data class AppTokenClaims(
  val installId: Long,
  val isInstallContext: Boolean,
  val userId: Long?,
  val projectId: Long?,
  val issuedAt: Date,
  val isReadOnly: Boolean,
)
