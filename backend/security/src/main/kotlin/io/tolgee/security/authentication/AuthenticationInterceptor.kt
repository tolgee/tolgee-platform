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
import io.tolgee.exceptions.PermissionException
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This interceptor validates the user authentication for use in the authorization phase.
 */
@Component
class AuthenticationInterceptor(
  private val authenticationFacade: AuthenticationFacade
) : HandlerInterceptor, Ordered {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler !is HandlerMethod) {
      return super.preHandle(request, response, handler)
    }

    val allowApiAccess = AnnotationUtils.getAnnotation(handler.method, AllowApiAccess::class.java)
    val requiresSuperAuth = requiresSuperAuthentication(request, handler)

    if (allowApiAccess == null && authenticationFacade.isApiAuthentication) {
      throw PermissionException(Message.API_ACCESS_FORBIDDEN)
    }

    if (allowApiAccess?.onlyPat == true && authenticationFacade.isProjectApiKeyAuth) {
      throw PermissionException(Message.INVALID_AUTHENTICATION_METHOD)
    }

    if (requiresSuperAuth && !authenticationFacade.isUserSuperAuthenticated) {
      throw PermissionException(Message.EXPIRED_SUPER_JWT_TOKEN)
    }

    return true
  }

  private fun requiresSuperAuthentication(request: HttpServletRequest, handlerMethod: HandlerMethod): Boolean {
    if (IS_ADMIN_RE.matches(request.requestURI)) return true
    return AnnotationUtils.getAnnotation(handlerMethod.method, RequiresSuperAuthentication::class.java) != null
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }

  companion object {
    // Admin routes
    // - /v2/administration/**
    // - /v2/ee-license/**
    val IS_ADMIN_RE = "^/v\\d+/(?:administration|ee-license).*$".toRegex()
  }
}
