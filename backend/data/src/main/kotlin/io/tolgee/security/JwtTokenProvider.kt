package io.tolgee.security

import io.tolgee.dtos.cacheable.UserAccountDto
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication

interface JwtTokenProvider {
  fun generateToken(userId: Long, isSuper: Boolean = false): JwtToken
  fun checkToken(authToken: JwtToken)
  fun getAuthentication(token: JwtToken): Authentication
  fun getUser(token: JwtToken): UserAccountDto
  fun resolveToken(req: HttpServletRequest): JwtToken?
  fun resolveToken(stringToken: String): JwtToken
  fun getAuthentication(jwtToken: String?): Authentication?
  fun generateToken(userId: Long, superExpiration: Long?): JwtToken
}
