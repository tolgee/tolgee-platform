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

import io.jsonwebtoken.*
import io.jsonwebtoken.security.SignatureException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.security.Key
import java.util.*

@Service
class JwtService(
  @Qualifier("jwt_signing_key")
  private val signingKey: Key,
  private val authenticationProperties: AuthenticationProperties,
  private val currentDateProvider: CurrentDateProvider,
  private val userAccountService: UserAccountService,
) {
  private val jwtParser: JwtParser = Jwts.parserBuilder()
    .setClock { currentDateProvider.date }
    .setSigningKey(signingKey)
    .build()

  /**
   * Emits an authentication token for the given user.
   *
   * @param userAccountId The user account ID this token belongs to.
   * @param isSuper Whether to emit a super-powered token or not.
   * @return An authentication token.
   */
  fun emitToken(userAccountId: Long, isSuper: Boolean = false): String {
    val now = currentDateProvider.date

    val expiration = Date(now.time + authenticationProperties.jwtExpiration)
    val builder = Jwts.builder()
      .signWith(signingKey)
      .setIssuedAt(now)
      .setAudience(JWT_TOKEN_AUDIENCE)
      .setSubject(userAccountId.toString())
      .setExpiration(expiration)

    if (isSuper) {
      val superExpiration = Date(now.time + authenticationProperties.jwtSuperExpiration)
      builder.claim(SUPER_JWT_TOKEN_EXPIRATION_CLAIM, superExpiration)
    }

    return builder.compact()
  }

  /**
   * Emits a ticket for a given user.
   *
   * @param userAccountId The user account ID this ticket belongs to.
   * @param ticketType The type of ticket.
   * @param expiresAfter Amount of milliseconds after which the ticket will be expired.
   * @return A ticket.
   */
  fun emitTicket(
    userAccountId: Long,
    ticketType: TicketType,
    expiresAfter: Long = DEFAULT_TICKET_EXPIRATION_TIME,
  ): String {
    val now = currentDateProvider.date

    return Jwts.builder()
      .signWith(signingKey)
      .setIssuedAt(now)
      .setAudience(JWT_TICKET_AUDIENCE)
      .setSubject(userAccountId.toString())
      .setExpiration(Date(now.time + expiresAfter))
      .claim(JWT_TICKET_TYPE_CLAIM, ticketType.name)
      .compact()
  }

  /**
   * Validates a token for a given user.
   *
   * @param token The JWT token to validate.
   * @return The authentication information.
   * @throws AuthenticationException The token is invalid or expired.
   */
  fun validateToken(token: String): TolgeeAuthentication {
    val jws = parseJwt(token)
    if (jws.body.audience != JWT_TOKEN_AUDIENCE) {
      // This is not a token - possibly a ticket or something else.
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    val account = validateJwt(jws.body)

    val steClaim = jws.body[SUPER_JWT_TOKEN_EXPIRATION_CLAIM] as? Long
    val hasSuperPowers = steClaim != null && steClaim > currentDateProvider.date.time

    return TolgeeAuthentication(
      jws,
      account,
      TolgeeAuthenticationDetails(hasSuperPowers)
    )
  }

  /**
   * Validates a ticket for a given user.
   *
   * @param token The JWT ticket to validate.
   * @param expectedType The expected ticket type for the operation.
   * @return The authenticated user account.
   * @throws AuthenticationException The ticket is invalid or expired.
   */
  fun validateTicket(token: String, expectedType: TicketType): UserAccount {
    val jws = parseJwt(token)
    if (jws.body.audience != JWT_TICKET_AUDIENCE) {
      // This is not a token - possibly a token or something else.
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    val rawJwsType = jws.body[JWT_TICKET_TYPE_CLAIM] as? String
      ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    val jwsType = TicketType.valueOf(rawJwsType)
    if (jwsType != expectedType) {
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    return validateJwt(jws.body)
  }

  private fun validateJwt(claims: Claims): UserAccount {
    val account = userAccountService.findActive(claims.subject.toLong())
      ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    if (account.tokensValidNotBefore != null && claims.issuedAt.before(account.tokensValidNotBefore)) {
      throw AuthenticationException(Message.EXPIRED_JWT_TOKEN)
    }

    return account
  }

  private fun parseJwt(token: String): Jws<Claims> {
    try {
      return jwtParser.parseClaimsJws(token)
    } catch (ex: SignatureException) {
      // Token signature is invalid - fake token (or key changed)
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    } catch (ex: MalformedJwtException) {
      // Token is malformed
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    } catch (ex: ExpiredJwtException) {
      // Token is expired
      throw AuthenticationException(Message.EXPIRED_JWT_TOKEN)
    } catch (ex: UnsupportedJwtException) {
      // Token is not of the correct type (e.g. attempting to use a plaintext token)
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    } catch (ex: IllegalArgumentException) {
      // Token does not contain claims
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }
  }

  companion object {
    const val JWT_TOKEN_AUDIENCE = "tg.tok"
    const val JWT_TICKET_AUDIENCE = "tg.tic"
    const val JWT_TICKET_TYPE_CLAIM = "t.typ"
    const val SUPER_JWT_TOKEN_EXPIRATION_CLAIM = "ste"

    const val DEFAULT_TICKET_EXPIRATION_TIME = 5 * 60 * 1000L // 5 minutes
  }

  enum class TicketType {
    AUTH_MFA,
    IMG_ACCESS,
  }
}
