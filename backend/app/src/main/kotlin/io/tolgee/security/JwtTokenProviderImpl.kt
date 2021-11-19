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
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.service.UserAccountService
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
  private val authenticationProvider: AuthenticationProvider
) : JwtTokenProvider {

  private val logger = LoggerFactory.getLogger(JwtTokenProviderImpl::class.java)

  private val key: SecretKey
    get() = Keys.hmacShaKeyFor(jwtSecretProvider.jwtSecret)

  override fun generateToken(userId: Long): JwtToken {
    val now = Date()
    return JwtToken(
      Jwts.builder()
        .setSubject(userId.toString())
        .setIssuedAt(Date())
        .setExpiration(Date(now.time + configuration.authentication.jwtExpiration))
        .signWith(key)
        .compact(),
      key
    )
  }

  override fun validateToken(authToken: JwtToken): Boolean {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken.toString())
      return true
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
    val user = getUser(token)
    return authenticationProvider.getAuthentication(user)
  }

  override fun getUser(token: JwtToken): UserAccountDto {
    return userAccountService.getDto(token.id)
      ?: throw AuthenticationException(io.tolgee.constants.Message.USER_NOT_FOUND)
  }

  override fun resolveToken(req: HttpServletRequest): JwtToken? {
    val bearerToken = req.getHeader("Authorization")
    return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      JwtToken(bearerToken.substring(7), key)
    } else null
  }

  override fun resolveToken(stringToken: String): JwtToken = JwtToken(stringToken, key)
}
