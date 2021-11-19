package io.tolgee.security

import io.tolgee.dtos.cacheable.UserAccountDto
import org.springframework.security.core.Authentication
import javax.servlet.http.HttpServletRequest

interface JwtTokenProvider {
  fun generateToken(userId: Long): JwtToken
  fun validateToken(authToken: JwtToken): Boolean
  fun getAuthentication(token: JwtToken): Authentication
  fun getUser(token: JwtToken): UserAccountDto
  fun resolveToken(req: HttpServletRequest): JwtToken?
  fun resolveToken(stringToken: String): JwtToken
}
