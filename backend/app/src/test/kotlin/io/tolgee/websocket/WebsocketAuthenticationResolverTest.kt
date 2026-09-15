package io.tolgee.websocket

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.authentication.DisabledAuthenticationResolver
import io.tolgee.security.oauth2.OAuth2AccessTokenResolver
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor

class WebsocketAuthenticationResolverTest {
  private val oauth2AccessTokenResolver = mock<OAuth2AccessTokenResolver>()
  private val disabledAuthenticationResolver = mock<DisabledAuthenticationResolver>()

  private val resolver =
    WebsocketAuthenticationResolver(
      jwtService = mock(),
      oauth2AccessTokenResolver = oauth2AccessTokenResolver,
      apiKeyService = mock(),
      patService = mock(),
      disabledAuthenticationResolver = disabledAuthenticationResolver,
      userAccountService = mock(),
      currentDateProvider = mock(),
    )

  @Test
  fun `a rejected credential resolves to no authentication`() {
    whenever(oauth2AccessTokenResolver.tryResolve(any()))
      .thenThrow(AuthenticationException(Message.INVALID_OAUTH_TOKEN))

    resolver.resolve(connectWithBearer()).assert.isNull()
  }

  @Test
  fun `a server-side failure is not mistaken for a rejected credential`() {
    whenever(oauth2AccessTokenResolver.tryResolve(any())).thenThrow(IllegalStateException("database is down"))

    assertThrows<IllegalStateException> { resolver.resolve(connectWithBearer()) }
  }

  private fun connectWithBearer(): StompHeaderAccessor =
    StompHeaderAccessor.create(StompCommand.CONNECT).apply {
      addNativeHeader("Authorization", "Bearer tgoat_token")
    }
}
