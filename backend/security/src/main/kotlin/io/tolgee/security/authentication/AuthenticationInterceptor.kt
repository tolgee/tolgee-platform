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

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.PermissionException
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * This interceptor validates the user authentication for use in the authorization phase.
 */
@Component
class AuthenticationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  private val authenticationProperties: AuthenticationProperties,
) : HandlerInterceptor,
  Ordered {
  override fun preHandle(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: Any,
  ): Boolean {
    if (handler !is HandlerMethod || DispatcherType.ASYNC == request.dispatcherType) {
      return super.preHandle(request, response, handler)
    }

    if (request.method == "OPTIONS") {
      // Do not process OPTIONS requests
      return true
    }

    val allowApiAccess = AnnotationUtils.getAnnotation(handler.method, AllowApiAccess::class.java)
    val requiresSuperAuth = requiresSuperAuthentication(handler)

    if (authenticationFacade.isApiAuthentication) {
      if (allowApiAccess == null) {
        throw PermissionException(Message.API_ACCESS_FORBIDDEN)
      }

      if (authenticationFacade.isPersonalAccessTokenAuth && !isPatAllowed(allowApiAccess)) {
        throw PermissionException(Message.PAT_ACCESS_NOT_ALLOWED)
      }

      if (authenticationFacade.isProjectApiKeyAuth && !isPakAllowed(allowApiAccess)) {
        throw PermissionException(Message.PAK_ACCESS_NOT_ALLOWED)
      }
    }

    if (
      requiresSuperAuth &&
      authenticationProperties.enabled &&
      authenticationFacade.authenticatedUser.needsSuperJwt &&
      !authenticationFacade.isUserSuperAuthenticated
    ) {
      throw PermissionException(Message.EXPIRED_SUPER_JWT_TOKEN)
    }

    return true
  }

  private fun requiresSuperAuthentication(handlerMethod: HandlerMethod): Boolean {
    return AnnotationUtils.getAnnotation(handlerMethod.method, RequiresSuperAuthentication::class.java) != null
  }

  private fun isPatAllowed(annotation: AllowApiAccess): Boolean {
    return annotation.tokenType == AuthTokenType.ANY || annotation.tokenType == AuthTokenType.ONLY_PAT
  }

  private fun isPakAllowed(annotation: AllowApiAccess): Boolean {
    return annotation.tokenType == AuthTokenType.ANY || annotation.tokenType == AuthTokenType.ONLY_PAK
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }
}
