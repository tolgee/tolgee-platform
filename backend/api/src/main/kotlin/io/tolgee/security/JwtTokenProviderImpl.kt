/*
 * Copyright (c) 2020. Tolgee
 */
package io.tolgee.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.JwtSecretProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.JwtToken.Companion.JWT_TOKEN_SUPER_EXPIRATION_CLAIM
import io.tolgee.service.security.UserAccountService
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey
import javax.servlet.http.HttpServletRequest

@Component
class JwtTokenProviderImpl(
  private val configuration: TolgeeProperties,
  private val userAccountService: UserAccountService,
  private val jwtSecretProvider: JwtSecretProvider,
  private val authenticationProvider: AuthenticationProvider,
  private val currentDateProvider: CurrentDateProvider,
  private val authenticationProperties: AuthenticationProperties
) : JwtTokenProvider {

  private val logger = LoggerFactory.getLogger(JwtTokenProviderImpl::class.java)

  private val key: SecretKey
    get() = Keys.hmacShaKeyFor(jwtSecretProvider.jwtSecret)

  /**
   * This generates a JWT token.
   * isSuper = true makes this key able to do sensitive operations (is JWT)
   * Super JWT can be returned only when 2FA is passed or is not set
   */
  override fun generateToken(userId: Long, isSuper: Boolean): JwtToken {
    val expirationTime = if (isSuper)
      currentDateProvider.date.time + authenticationProperties.jwtSuperExpiration
    else null

    return generateToken(
      userId,
      expirationTime
    )
  }

  /**
   * This generates a JWT token.
   * superExpiration = when this tokens superiority expires
   * Super JWT can be returned only when 2FA is passed or is not set
   */
  override fun generateToken(userId: Long, superExpiration: Long?): JwtToken {
    val claims = mutableMapOf<String, Any>()
    if (superExpiration != null) {
      // super key is needed for sensitive operations
      claims[JWT_TOKEN_SUPER_EXPIRATION_CLAIM] = superExpiration
    }
    val now = Date()
    return JwtToken(
      Jwts.builder()
        .setSubject(userId.toString())
        .setIssuedAt(Date())
        .addClaims(claims)
        .setExpiration(Date(now.time + configuration.authentication.jwtExpiration))
        .signWith(key)
        .compact(),
      key
    )
  }

  override fun checkToken(authToken: JwtToken) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken.toString())
      return
    } catch (ex: SignatureException) {
      logger.error("Invalid JWT signature")
    } catch (ex: MalformedJwtException) {
      throw AuthenticationException(io.tolgee.constants.Message.INVALID_JWT_TOKEN)
    } catch (ex: ExpiredJwtException) {
      throw AuthenticationException(io.tolgee.constants.Message.EXPIRED_JWT_TOKEN)
    } catch (ex: UnsupportedJwtException) {
      logger.error("Unsupported JWT token")
    } catch (ex: IllegalArgumentException) {
      logger.error("JWT claims string is empty.")
    }
    throw AuthenticationException(io.tolgee.constants.Message.GENERAL_JWT_ERROR)
  }

  override fun getAuthentication(token: JwtToken): Authentication {
    this.checkToken(token)
    val user = getUser(token)
    return authenticationProvider.getAuthentication(user)
  }

  override fun getUser(token: JwtToken): UserAccountDto {
    val userAccount = userAccountService.findActive(token.id)
      ?: throw AuthenticationException(io.tolgee.constants.Message.USER_NOT_FOUND)

    if (userAccount.tokensValidNotBefore != null && token.issuedAt.before(userAccount.tokensValidNotBefore))
      throw AuthenticationException(io.tolgee.constants.Message.EXPIRED_JWT_TOKEN)

    return UserAccountDto.fromEntity(userAccount)
  }

  override fun resolveToken(req: HttpServletRequest): JwtToken? {
    val bearerToken = req.getHeader("Authorization")
    return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      JwtToken(bearerToken.substring(7), key)
    } else null
  }

  override fun resolveToken(stringToken: String): JwtToken = JwtToken(stringToken, key)

  override fun getAuthentication(jwtToken: String?): Authentication? {
    val token = jwtToken?.let { this.resolveToken(jwtToken) }
    if (token != null) {
      return this.getAuthentication(token)
    }
    return null
  }
}
