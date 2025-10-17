/**
 * Copyright (C) 2025 Tolgee s.r.o. and contributors
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
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authorization.AbstractAuthorizationInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * Checks if a user has admin privileges.
 * Blocks write requests when the user is only a supporter.
 * Annotate class or method with [ReadOnlyOperation] or [WriteOperation] to override.
 */
@Component
class AdminAccessInterceptor(
  private val authenticationFacade: AuthenticationFacade,
) : AbstractAuthorizationInterceptor(allowGlobalRoutes = false) {
  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    if (!authenticationFacade.isAuthenticated) {
      // If not authenticated, skip (let other interceptors handle)
      return true
    }

    val hasWriteAccess = authenticationFacade.authenticatedUser.isAdmin()
    if (hasWriteAccess) {
      // If not in read-only mode, allow
      return true
    }

    val hasReadAccess = authenticationFacade.authenticatedUser.isSupporterOrAdmin()
    if (hasReadAccess && handler.isReadOnly(request.method)) {
      // These methods should be read-only - safe to call
      return true
    }

    throw PermissionException(Message.OPERATION_NOT_PERMITTED)
  }
}
