/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

/**
 * Negative gates of the public-client refresh path. The converter must only ever intercept a bare `client_id` on the
 * refresh grant (never the code exchange, never a secret-bearing request); the provider must reject any client that is
 * not a registered public client holding the refresh grant.
 */
class PublicClientRefreshAuthenticationTest {
  private val converter = PublicClientRefreshAuthenticationConverter()

  private fun refreshRequest(): MockHttpServletRequest =
    MockHttpServletRequest().apply {
      setParameter("grant_type", AuthorizationGrantType.REFRESH_TOKEN.value)
      setParameter("client_id", "public-client")
    }

  @Test
  fun `converter ignores the authorization-code grant`() {
    val request =
      MockHttpServletRequest().apply {
        setParameter("grant_type", AuthorizationGrantType.AUTHORIZATION_CODE.value)
        setParameter("client_id", "public-client")
      }
    assertThat(converter.convert(request)).isNull()
  }

  @Test
  fun `converter ignores a request carrying an Authorization header`() {
    val request = refreshRequest().apply { addHeader("Authorization", "Basic abc") }
    assertThat(converter.convert(request)).isNull()
  }

  @Test
  fun `converter ignores a request carrying a client_secret`() {
    val request = refreshRequest().apply { setParameter("client_secret", "shhh") }
    assertThat(converter.convert(request)).isNull()
  }

  @Test
  fun `converter ignores a request without a client_id`() {
    val request =
      MockHttpServletRequest().apply { setParameter("grant_type", AuthorizationGrantType.REFRESH_TOKEN.value) }
    assertThat(converter.convert(request)).isNull()
  }

  @Test
  fun `converter accepts a bare client_id on the refresh grant`() {
    val token = converter.convert(refreshRequest()) as OAuth2ClientAuthenticationToken
    assertThat(token.principal).isEqualTo("public-client")
    assertThat(token.clientAuthenticationMethod).isEqualTo(ClientAuthenticationMethod.NONE)
  }

  private fun publicRefreshClient(): RegisteredClient =
    RegisteredClient
      .withId("id")
      .clientId("public-client")
      .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .redirectUri("https://example.com/cb")
      .build()

  private fun provider(client: RegisteredClient?): PublicClientRefreshAuthenticationProvider {
    val repo = mock<RegisteredClientRepository>()
    whenever(repo.findByClientId("public-client")).thenReturn(client)
    return PublicClientRefreshAuthenticationProvider(repo)
  }

  private fun refreshToken(): OAuth2ClientAuthenticationToken =
    OAuth2ClientAuthenticationToken(
      "public-client",
      ClientAuthenticationMethod.NONE,
      null,
      mapOf("grant_type" to AuthorizationGrantType.REFRESH_TOKEN.value),
    )

  @Test
  fun `provider authenticates a registered public refresh client`() {
    val result = provider(publicRefreshClient()).authenticate(refreshToken()) as OAuth2ClientAuthenticationToken
    assertThat(result.isAuthenticated).isTrue()
    assertThat(result.registeredClient?.clientId).isEqualTo("public-client")
  }

  @Test
  fun `provider rejects an unknown client`() {
    assertThatThrownBy { provider(null).authenticate(refreshToken()) }
      .isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun `provider rejects a confidential client`() {
    val confidential =
      RegisteredClient
        .withId("id")
        .clientId("public-client")
        .clientSecret("secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://example.com/cb")
        .build()
    assertThatThrownBy { provider(confidential).authenticate(refreshToken()) }
      .isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun `provider rejects a public client without the refresh grant`() {
    val noRefresh =
      RegisteredClient
        .withId("id")
        .clientId("public-client")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://example.com/cb")
        .build()
    assertThatThrownBy { provider(noRefresh).authenticate(refreshToken()) }
      .isInstanceOf(OAuth2AuthenticationException::class.java)
  }

  @Test
  fun `provider does not handle a non-refresh grant token`() {
    val codeGrantToken =
      OAuth2ClientAuthenticationToken(
        "public-client",
        ClientAuthenticationMethod.NONE,
        null,
        mapOf("grant_type" to AuthorizationGrantType.AUTHORIZATION_CODE.value),
      )
    assertThat(provider(publicRefreshClient()).authenticate(codeGrantToken)).isNull()
  }
}
