/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
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

package io.tolgee.security.authentication

import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.Date

class JwtServiceTest {
  companion object {
    const val TEST_USER_ID = 1337L
    const val TEST_USER_EMAIL = "meow@meow.test"
    const val USER_TOKENS_SINCE_RELATIVE = 10 * 1000L // 10 seconds

    const val SUPER_JWT_LIFETIME = 30 * 1000L // 30 seconds
    const val JWT_LIFETIME = 60 * 1000L // 60 seconds

    const val ADMIN_ACTOR_ID = 4242L
    const val SUPPORTER_ACTOR_ID = 4343L
  }

  private val testSigningKey = Keys.secretKeyFor(SignatureAlgorithm.HS256)

  private val authenticationProperties = Mockito.mock(AuthenticationProperties::class.java)

  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val userAccountService = Mockito.mock(UserAccountService::class.java)

  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)
  private val adminActor = Mockito.mock(UserAccountDto::class.java)
  private val supporterActor = Mockito.mock(UserAccountDto::class.java)

  private val jwtService: JwtService =
    JwtService(
      testSigningKey,
      authenticationProperties,
      currentDateProvider,
      userAccountService,
      authenticationFacade,
    )

  @BeforeEach
  fun setupMocks() {
    val now = Date()

    Mockito.`when`(authenticationProperties.jwtExpiration).thenReturn(JWT_LIFETIME)
    Mockito.`when`(authenticationProperties.jwtSuperExpiration).thenReturn(SUPER_JWT_LIFETIME)

    Mockito.`when`(currentDateProvider.date).thenReturn(now)

    Mockito.`when`(userAccountService.findDto(Mockito.anyLong())).thenReturn(null)
    Mockito.`when`(userAccountService.findDto(TEST_USER_ID)).thenReturn(userAccount)
    Mockito.`when`(userAccountService.findDto(ADMIN_ACTOR_ID)).thenReturn(adminActor)
    Mockito.`when`(userAccountService.findDto(SUPPORTER_ACTOR_ID)).thenReturn(supporterActor)

    Mockito.`when`(userAccount.id).thenReturn(TEST_USER_ID)
    Mockito.`when`(userAccount.username).thenReturn(TEST_USER_EMAIL)
    Mockito.`when`(userAccount.tokensValidNotBefore).thenReturn(Date(now.time - USER_TOKENS_SINCE_RELATIVE))
    Mockito.`when`(adminActor.role).thenReturn(UserAccount.Role.USER)

    Mockito.`when`(adminActor.id).thenReturn(ADMIN_ACTOR_ID)
    Mockito.`when`(adminActor.username).thenReturn("admin@tolgee.test")
    Mockito.`when`(adminActor.role).thenReturn(UserAccount.Role.ADMIN)

    Mockito.`when`(supporterActor.id).thenReturn(SUPPORTER_ACTOR_ID)
    Mockito.`when`(supporterActor.username).thenReturn("supporter@tolgee.test")
    Mockito.`when`(supporterActor.role).thenReturn(UserAccount.Role.SUPPORTER)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authenticationProperties, currentDateProvider, userAccountService, authenticationFacade, userAccount)
  }

  @Test
  fun `it generates and understands tokens`() {
    val token = jwtService.emitToken(TEST_USER_ID)
    val auth = jwtService.validateToken(token)

    assertThat(auth.principal.id).isEqualTo(TEST_USER_ID)
    assertThat(auth.principal.username).isEqualTo(TEST_USER_EMAIL)
  }

  @Test
  fun `it generates and understands tickets`() {
    val ticket = jwtService.emitTicket(TEST_USER_ID, JwtService.TicketType.AUTH_MFA)
    val authenticated = jwtService.validateTicket(ticket, JwtService.TicketType.AUTH_MFA)

    assertThat(authenticated.data).isNull()
    assertThat(authenticated.userAccount.id).isEqualTo(TEST_USER_ID)
    assertThat(authenticated.userAccount.username).isEqualTo(TEST_USER_EMAIL)
  }

  @Test
  fun `it stores the super powers of tokens when it has them`() {
    val token = jwtService.emitToken(TEST_USER_ID)
    val superToken = jwtService.emitToken(TEST_USER_ID, isSuper = true)

    val auth = jwtService.validateToken(token)
    val superAuth = jwtService.validateToken(superToken)

    assertThat(auth.isSuperToken).isFalse()
    assertThat(superAuth.isSuperToken).isTrue()
  }

  @Test
  fun `it ignores super powers when they are expired`() {
    val now = currentDateProvider.date.time
    val superToken = jwtService.emitToken(TEST_USER_ID, isSuper = true)

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(now + SUPER_JWT_LIFETIME + 1000))

    val superAuth = jwtService.validateToken(superToken)
    assertThat(superAuth.isSuperToken).isFalse()
  }

  @Test
  fun `it validates the type of the ticket`() {
    val ticket = jwtService.emitTicket(TEST_USER_ID, JwtService.TicketType.AUTH_MFA)

    assertDoesNotThrow { jwtService.validateTicket(ticket, JwtService.TicketType.AUTH_MFA) }
    assertThrows<AuthenticationException> { jwtService.validateTicket(ticket, JwtService.TicketType.IMG_ACCESS) }
  }

  @Test
  fun `it stores arbitrary data in tickets`() {
    val data = mapOf("owo" to "uwu", "meow" to "nya", "testing" to "yes yes")

    val ticket = jwtService.emitTicket(TEST_USER_ID, JwtService.TicketType.AUTH_MFA, data = data)
    val authenticated = jwtService.validateTicket(ticket, JwtService.TicketType.AUTH_MFA)

    assertThat(authenticated.data).isNotNull
    assertThat(authenticated.data).isEqualTo(data)
  }

  @Test
  fun `it rejects expired tokens`() {
    val now = currentDateProvider.date.time
    val token = jwtService.emitToken(TEST_USER_ID)

    assertDoesNotThrow { jwtService.validateToken(token) }

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(now + JWT_LIFETIME + 1000))

    assertThrows<AuthenticationException> { jwtService.validateToken(token) }
  }

  @Test
  fun `it rejects tokens emitted before user tokens validity period`() {
    val now = currentDateProvider.date.time
    val token = jwtService.emitToken(TEST_USER_ID)

    assertDoesNotThrow { jwtService.validateToken(token) }

    Mockito.`when`(userAccount.tokensValidNotBefore).thenReturn(Date(now + 10_000))

    assertThrows<AuthenticationException> { jwtService.validateToken(token) }
  }

  @Test
  fun `it rejects expired tickets`() {
    val now = currentDateProvider.date.time
    val longTicketExpiry = JwtService.DEFAULT_TICKET_EXPIRATION_TIME + 10000

    val ticket = jwtService.emitTicket(TEST_USER_ID, JwtService.TicketType.AUTH_MFA)
    val ticketLong = jwtService.emitTicket(TEST_USER_ID, JwtService.TicketType.AUTH_MFA, longTicketExpiry)

    assertDoesNotThrow { jwtService.validateTicket(ticket, JwtService.TicketType.AUTH_MFA) }
    assertDoesNotThrow { jwtService.validateTicket(ticketLong, JwtService.TicketType.AUTH_MFA) }

    Mockito.`when`(currentDateProvider.date).thenReturn(
      Date(now + JwtService.DEFAULT_TICKET_EXPIRATION_TIME + 1000),
    )

    assertThrows<AuthenticationException> { jwtService.validateTicket(ticket, JwtService.TicketType.AUTH_MFA) }
    assertDoesNotThrow { jwtService.validateTicket(ticketLong, JwtService.TicketType.AUTH_MFA) }

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(now + longTicketExpiry + 1000))

    assertThrows<AuthenticationException> { jwtService.validateTicket(ticket, JwtService.TicketType.AUTH_MFA) }
    assertThrows<AuthenticationException> { jwtService.validateTicket(ticketLong, JwtService.TicketType.AUTH_MFA) }
  }

  @Test
  fun `it rejects invalid tokens and tickets`() {
    val invalidToken = "this certainly does not look like a token to me!"
    val badToken =
      "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJzdWIiOiIxMzM3IiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE5MTYyMzkwMjJ9.kn_amo5h7__tlveBus_215x3Zq9UGFI6O_QpJ2rKi9o"
    val noSigToken =
      "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0." +
        "eyJzdWIiOiIxMzM3IiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE5MTYyMzkwMjJ9"
    val token = jwtService.emitToken(TEST_USER_ID)
    val ticket = jwtService.emitTicket(TEST_USER_ID, JwtService.TicketType.AUTH_MFA)

    assertThrows<AuthenticationException> { jwtService.validateToken(invalidToken) }
    assertThrows<AuthenticationException> { jwtService.validateToken(badToken) }
    assertThrows<AuthenticationException> { jwtService.validateToken(noSigToken) }
    assertThrows<AuthenticationException> { jwtService.validateToken(ticket) }

    assertThrows<AuthenticationException> { jwtService.validateTicket(invalidToken, JwtService.TicketType.AUTH_MFA) }
    assertThrows<AuthenticationException> { jwtService.validateTicket(badToken, JwtService.TicketType.AUTH_MFA) }
    assertThrows<AuthenticationException> { jwtService.validateTicket(noSigToken, JwtService.TicketType.AUTH_MFA) }
    assertThrows<AuthenticationException> { jwtService.validateTicket(token, JwtService.TicketType.AUTH_MFA) }
  }

  @Test
  fun `it sets read-only flag in tokens`() {
    val token = jwtService.emitToken(TEST_USER_ID, isReadOnly = true)
    val auth = jwtService.validateToken(token)

    assertThat(auth.isReadOnly).isTrue()
    assertThat(auth.actingAsUserAccount).isNull()
  }

  @Test
  fun `it carries actor information for admin actor`() {
    val token = jwtService.emitToken(TEST_USER_ID, actingAsUserAccountId = ADMIN_ACTOR_ID)
    val auth = jwtService.validateToken(token)

    assertThat(auth.isReadOnly).isFalse()
    assertThat(auth.actingAsUserAccount).isNotNull
    assertThat(auth.actingAsUserAccount!!.id).isEqualTo(ADMIN_ACTOR_ID)
  }

  @Test
  fun `it rejects read-only tokens for admin subject`() {
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.ADMIN)

    val token = jwtService.emitToken(TEST_USER_ID, isReadOnly = true)

    assertThrows<AuthenticationException> { jwtService.validateToken(token) }
  }

  @Test
  fun `it rejects supporter actor impersonation when not read-only`() {
    val token = jwtService.emitToken(TEST_USER_ID, actingAsUserAccountId = SUPPORTER_ACTOR_ID, isReadOnly = false)

    assertThrows<AuthenticationException> { jwtService.validateToken(token) }
  }

  @Test
  fun `it allows supporter actor impersonation when read-only`() {
    val token = jwtService.emitToken(TEST_USER_ID, actingAsUserAccountId = SUPPORTER_ACTOR_ID, isReadOnly = true)
    val auth = jwtService.validateToken(token)

    assertThat(auth.isReadOnly).isTrue()
    assertThat(auth.actingAsUserAccount).isNotNull
    assertThat(auth.actingAsUserAccount!!.id).isEqualTo(SUPPORTER_ACTOR_ID)
  }

  @Test
  fun `it refreshes token and propagates read-only, super and actor`() {
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.actingUser).thenReturn(supporterActor)
    Mockito.`when`(authenticationFacade.isReadOnly).thenReturn(true)
    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(false)

    val token = jwtService.emitTokenRefreshForCurrentUser(isSuper = null)
    val auth = jwtService.validateToken(token)

    assertThat(auth.isReadOnly).isTrue()
    assertThat(auth.actingAsUserAccount).isNotNull
    assertThat(auth.actingAsUserAccount!!.id).isEqualTo(SUPPORTER_ACTOR_ID)
    assertThat(auth.isSuperToken).isFalse()
  }
}
