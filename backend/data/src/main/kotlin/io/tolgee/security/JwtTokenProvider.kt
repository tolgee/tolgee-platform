package io.tolgee.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.tolgee.dtos.cacheable.UserAccountDto
import org.springframework.security.core.Authentication
import javax.servlet.http.HttpServletRequest

interface JwtTokenProvider {
  fun generateToken(userId: Long, isSuper: Boolean = false): JwtToken
  fun validateToken(authToken: JwtToken): Jws<Claims>?
  fun getAuthentication(token: JwtToken): Authentication
  fun getUser(token: JwtToken): UserAccountDto
  fun resolveToken(req: HttpServletRequest): JwtToken?
  fun resolveToken(stringToken: String): JwtToken
  fun getAuthentication(jwtToken: String?): Authentication?
  fun generateToken(userId: Long, superExpiration: Long?): JwtToken
}
