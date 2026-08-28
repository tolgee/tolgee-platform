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

  fun mintUserContextToken(
    installId: Long,
    userId: Long,
    isReadOnly: Boolean,
  ): String {
    val builder =
      baseBuilder(installId)
        .setSubject(userId.toString())
        .claim(JWT_APP_TOKEN_CONTEXT_CLAIM, CONTEXT_USER)

    if (isReadOnly) {
      builder.claim(JWT_APP_TOKEN_READ_ONLY_CLAIM, true)
    }

    return builder.compact()
  }

  fun mintInstallContextToken(installId: Long): String {
    return baseBuilder(installId)
      .claim(JWT_APP_TOKEN_CONTEXT_CLAIM, CONTEXT_INSTALL)
      .compact()
  }

  fun mintAppLevelToken(appId: Long): String {
    return Jwts
      .builder()
      .signWith(signingKey)
      .setIssuedAt(currentDateProvider.date)
      .setAudience(JWT_APP_TOKEN_AUDIENCE)
      .setExpiration(Date(currentDateProvider.date.time + appsProperties.tokenExpiration))
      .claim(JWT_APP_TOKEN_CONTEXT_CLAIM, CONTEXT_APP)
      .claim(JWT_APP_TOKEN_APP_ID_CLAIM, appId)
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

    if (jws.body[JWT_APP_TOKEN_CONTEXT_CLAIM] == CONTEXT_APP) {
      val appId =
        (jws.body[JWT_APP_TOKEN_APP_ID_CLAIM] as? Number)?.toLong()
          ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)
      return AppTokenClaims(
        installId = null,
        appId = appId,
        isInstallContext = false,
        isAppContext = true,
        userId = null,
        issuedAt = jws.body.issuedAt,
        isReadOnly = false,
      )
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
        issuedAt = jws.body.issuedAt,
        isReadOnly = isReadOnly,
      )
    }

    val userId =
      jws.body.subject?.toLongOrNull()
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    return AppTokenClaims(
      installId = installId,
      isInstallContext = false,
      userId = userId,
      issuedAt = jws.body.issuedAt,
      isReadOnly = isReadOnly,
    )
  }

  companion object {
    const val JWT_APP_TOKEN_AUDIENCE = "tg.app"
    const val JWT_APP_TOKEN_INSTALL_ID_CLAIM = "tg.app.inst"
    const val JWT_APP_TOKEN_APP_ID_CLAIM = "tg.app.app"
    const val JWT_APP_TOKEN_CONTEXT_CLAIM = "tg.app.ctx"
    const val JWT_APP_TOKEN_READ_ONLY_CLAIM = "tg.app.ro"
    const val CONTEXT_USER = "user"
    const val CONTEXT_INSTALL = "install"
    const val CONTEXT_APP = "app"
  }
}

data class AppTokenClaims(
  val installId: Long?,
  val isInstallContext: Boolean,
  val userId: Long?,
  val issuedAt: Date,
  val isReadOnly: Boolean,
  val appId: Long? = null,
  val isAppContext: Boolean = false,
)
