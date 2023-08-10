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

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.ratelimit.RateLimitProperties
import io.tolgee.security.ratelimit.RateLimitService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthenticationFilter(
  private val authenticationProperties: AuthenticationProperties,
  private val rateLimitService: RateLimitService,
  private val jwtService: JwtService,
) : OncePerRequestFilter() {
  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    val policy = rateLimitService.getIpAuthRateLimitPolicy(request)
    var succeed = false

    if (policy == null) {
      succeed = doAuthenticate(request)
    } else {
      rateLimitService.consumeBucketUnless(policy) {
        succeed = doAuthenticate(request)
        succeed
      }
    }

    if (!succeed) {
      throw AuthenticationException(Message.UNAUTHENTICATED)
    }

    filterChain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return !authenticationProperties.enabled
  }

  private fun doAuthenticate(request: HttpServletRequest): Boolean {
    val authorization = request.getHeader("Authorization")
    if (authorization?.startsWith("Bearer ") == true) {
      val auth = jwtService.validateToken(authorization.substring(7))
      SecurityContextHolder.getContext().authentication = auth
      return true
    }

    val apiKey = request.getHeader("X-API-Key") ?: request.getParameter("ak")
    if (apiKey != null) {
      TODO("API Key validation")
    }

    return false
  }
}
