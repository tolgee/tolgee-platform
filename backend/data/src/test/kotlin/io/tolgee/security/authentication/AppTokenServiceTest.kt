package io.tolgee.security.authentication

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
