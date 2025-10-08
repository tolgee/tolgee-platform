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

package io.tolgee.security.authorization

import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.isReadOnly
import io.tolgee.service.organization.OrganizationRoleService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * This interceptor performs an authorization step to access organization-related endpoints.
 * By default, the user needs to have access to at least 1 project on the target org to access it.
 */
@Component
class OrganizationAuthorizationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  @Lazy
  private val organizationRoleService: OrganizationRoleService,
  @Lazy
  private val requestContextService: RequestContextService,
  private val organizationHolder: OrganizationHolder,
) : AbstractAuthorizationInterceptor() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    val userId = authenticationFacade.authenticatedUser.id
    val organization =
      requestContextService.getTargetOrganization(request)
        // Two possible scenarios: we're on `GET/POST /v2/organization`, or the organization was not found.
        // In both cases, there is no authorization to perform, and we simply continue.
        // It is not the job of the interceptor to return a 404 error.
        ?: return true

    var bypassed = false
    val requiredRole = getRequiredRole(request, handler)
    logger.debug(
      "Checking access to org#{} by user#{} (Requires {})",
      organization.id,
      userId,
      requiredRole ?: "read-only",
    )

    if (!organizationRoleService.canUserViewStrict(userId, organization.id)) {
      if (!canBypass(request, handler)) {
        logger.debug(
          "Rejecting access to org#{} for user#{} - No view permissions",
          organization.id,
          userId,
        )

        if (!canBypassForReadOnly()) {
          // Security consideration: if the user cannot see the organization, pretend it does not exist.
          throw NotFoundException()
        }

        // Admin access for read-only operations is allowed, but it's not enough for the current operation.
        throw PermissionException()
      }

      bypassed = true
    }

    if (requiredRole != null && !organizationRoleService.isUserOfRole(userId, organization.id, requiredRole)) {
      if (!canBypass(request, handler)) {
        logger.debug(
          "Rejecting access to org#{} for user#{} - Insufficient role",
          organization.id,
          userId,
        )

        throw PermissionException()
      }

      bypassed = true
    }

    if (bypassed) {
      logger.info(
        "Use of admin privileges: user#{} failed local security checks for org#{} - bypassing for {} {}",
        userId,
        organization.id,
        request.method,
        request.requestURI,
      )
    }

    organizationHolder.organization = organization
    return true
  }

  private fun getRequiredRole(
    request: HttpServletRequest,
    handler: HandlerMethod,
  ): OrganizationRoleType? {
    val defaultPerms = AnnotationUtils.getAnnotation(handler.method, UseDefaultPermissions::class.java)
    val orgPermission = AnnotationUtils.getAnnotation(handler.method, RequiresOrganizationRole::class.java)

    if (defaultPerms == null && orgPermission == null) {
      // A permission policy MUST be explicitly defined.
      throw RuntimeException("No permission policy have been set for URI ${request.requestURI}!")
    }

    if (defaultPerms != null && orgPermission != null) {
      // Policy doesn't make sense
      throw RuntimeException(
        "Both `@UseDefaultPermissions` and `@RequiresOrganizationRole` have been set for this endpoint!",
      )
    }

    return orgPermission?.role
  }

  private fun canBypass(
    request: HttpServletRequest,
    handler: HandlerMethod,
  ): Boolean {
    if (authenticationFacade.authenticatedUser.isAdmin()) {
      return true
    }

    val forReadOnly = handler.isReadOnly(request.method)
    return forReadOnly && canBypassForReadOnly()
  }

  private fun canBypassForReadOnly(): Boolean {
    return authenticationFacade.authenticatedUser.isSupporterOrAdmin()
  }
}
