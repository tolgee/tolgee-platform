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
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.authorization.AbstractAuthorizationInterceptor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * Blocks write requests when the current authentication is read-only.
 * Annotate class or method with [ReadOnlyOperation] or [WriteOperation] to override.
 */
@Component
class ReadOnlyModeInterceptor(
  private val authenticationFacade: AuthenticationFacade,
) : AbstractAuthorizationInterceptor(allowGlobalRoutes = false) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    if (!authenticationFacade.isAuthenticated) {
      // If not authenticated, skip (let other interceptors handle)
      return true
    }

    if (!authenticationFacade.isReadOnly) {
      // If not in read-only mode, allow
      return true
    }

    if (handler.isReadOnly(request.method)) {
      // These methods should be read-only - safe to call from read-only mode
      return true
    }

    logger.debug(
      "Rejecting access for user#{} - Write operation is not allowed in read-only mode",
      authenticationFacade.authenticatedUser.id,
    )
    throw PermissionException(Message.OPERATION_NOT_PERMITTED_IN_READ_ONLY_MODE)
  }
}
