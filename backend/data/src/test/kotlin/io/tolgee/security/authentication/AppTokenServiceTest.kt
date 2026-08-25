package io.tolgee.security.authentication

import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AppsProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.Date

class AppTokenServiceTest {
  private val signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)

  private val appsProperties = AppsProperties().apply { tokenExpiration = TOKEN_LIFETIME }

  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val appTokenService = AppTokenService(signingKey, appsProperties, currentDateProvider)

  @BeforeEach
  fun setup() {
    Mockito.`when`(currentDateProvider.date).thenReturn(Date(NOW))
  }

  @Test
  fun `carries the read-only flag of the minting session`() {
    val token = mintUserToken(isReadOnly = true)
    appTokenService
      .validateToken(token)
      .isReadOnly.assert
      .isTrue()
  }

  @Test
  fun `defaults to read-write`() {
    val token = mintUserToken(isReadOnly = false)
    appTokenService
      .validateToken(token)
      .isReadOnly.assert
      .isFalse()
  }

  @Test
  fun `an install-context token is read-write`() {
    val token = appTokenService.mintInstallContextToken(INSTALL_ID)
    val claims = appTokenService.validateToken(token)
    claims.isInstallContext.assert.isTrue()
    claims.isReadOnly.assert.isFalse()
  }

  @Test
  fun `reports an expired token as expired`() {
    val token = mintUserToken(isReadOnly = false)
    Mockito.`when`(currentDateProvider.date).thenReturn(Date(NOW + TOKEN_LIFETIME + 10_000))

    val exception = assertThrows<AuthExpiredException> { appTokenService.validateToken(token) }
    exception.code.assert.isEqualTo(Message.EXPIRED_JWT_TOKEN.code)
  }

  @Test
  fun `rejects a token signed with a foreign key`() {
    val foreign = AppTokenService(Keys.secretKeyFor(SignatureAlgorithm.HS256), appsProperties, currentDateProvider)
    val token = foreign.mintInstallContextToken(INSTALL_ID)

    val exception = assertThrows<AuthenticationException> { appTokenService.validateToken(token) }
    exception.code.assert.isEqualTo(Message.INVALID_JWT_TOKEN.code)
  }

  @Test
  fun `rejects a token whose audience is not the apps audience`() {
    val token =
      rawToken {
        setAudience("not.tg.app")
        claim(AppTokenService.JWT_APP_TOKEN_INSTALL_ID_CLAIM, INSTALL_ID)
        claim(AppTokenService.JWT_APP_TOKEN_CONTEXT_CLAIM, AppTokenService.CONTEXT_INSTALL)
      }
    assertThrows<AuthenticationException> { appTokenService.validateToken(token) }
      .code.assert
      .isEqualTo(Message.INVALID_JWT_TOKEN.code)
  }

  @Test
  fun `rejects a token that carries no install id`() {
    val token =
      rawToken {
        setAudience(AppTokenService.JWT_APP_TOKEN_AUDIENCE)
        claim(AppTokenService.JWT_APP_TOKEN_CONTEXT_CLAIM, AppTokenService.CONTEXT_INSTALL)
      }
    assertThrows<AuthenticationException> { appTokenService.validateToken(token) }
      .code.assert
      .isEqualTo(Message.INVALID_JWT_TOKEN.code)
  }

  @Test
  fun `treats a token with no context claim as user context`() {
    val token =
      rawToken {
        setAudience(AppTokenService.JWT_APP_TOKEN_AUDIENCE)
        setSubject(USER_ID.toString())
        claim(AppTokenService.JWT_APP_TOKEN_INSTALL_ID_CLAIM, INSTALL_ID)
        claim(AppTokenService.JWT_APP_TOKEN_PROJECT_ID_CLAIM, PROJECT_ID)
      }
    appTokenService
      .validateToken(token)
      .isInstallContext.assert
      .isFalse()
  }

  @Test
  fun `rejects a user-context token with no subject`() {
    val token =
      rawToken {
        setAudience(AppTokenService.JWT_APP_TOKEN_AUDIENCE)
        claim(AppTokenService.JWT_APP_TOKEN_INSTALL_ID_CLAIM, INSTALL_ID)
        claim(AppTokenService.JWT_APP_TOKEN_PROJECT_ID_CLAIM, PROJECT_ID)
      }
    assertThrows<AuthenticationException> { appTokenService.validateToken(token) }
      .code.assert
      .isEqualTo(Message.INVALID_JWT_TOKEN.code)
  }

  @Test
  fun `rejects a user-context token with no project id`() {
    val token =
      rawToken {
        setAudience(AppTokenService.JWT_APP_TOKEN_AUDIENCE)
        setSubject(USER_ID.toString())
        claim(AppTokenService.JWT_APP_TOKEN_INSTALL_ID_CLAIM, INSTALL_ID)
      }
    assertThrows<AuthenticationException> { appTokenService.validateToken(token) }
      .code.assert
      .isEqualTo(Message.INVALID_JWT_TOKEN.code)
  }

  private fun rawToken(configure: JwtBuilder.() -> Unit): String =
    Jwts
      .builder()
      .signWith(signingKey)
      .setIssuedAt(Date(NOW))
      .setExpiration(Date(NOW + TOKEN_LIFETIME))
      .apply(configure)
      .compact()

  private fun mintUserToken(isReadOnly: Boolean): String {
    return appTokenService.mintUserContextToken(
      installId = INSTALL_ID,
      userId = USER_ID,
      projectId = PROJECT_ID,
      isReadOnly = isReadOnly,
    )
  }

  companion object {
    private const val NOW = 1_700_000_000_000L
    private const val TOKEN_LIFETIME = 60 * 1000L
    private const val INSTALL_ID = 42L
    private const val USER_ID = 1337L
    private const val PROJECT_ID = 7L
  }
}
