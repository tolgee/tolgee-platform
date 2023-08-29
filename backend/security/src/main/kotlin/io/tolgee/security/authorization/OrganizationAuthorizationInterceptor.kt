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

import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This interceptor performs authorization step to access organization-related endpoints.
 * By default, the user needs to have access to at least 1 project on the target org to access it.
 */
@Component
class OrganizationAuthorizationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val requestContextService: RequestContextService,
) : HandlerInterceptor, Ordered {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler !is HandlerMethod) {
      return super.preHandle(request, response, handler)
    }

    val userId = authenticationFacade.authenticatedUser.id
    val organization = requestContextService.getTargetOrganization(request)
      // Two possible scenarios: we're on `GET/POST /v2/organization`, or the organization was not found.
      // In both cases, there is no authorization to perform and we simply continue.
      // It is not the job of the interceptor to return a 404 error.
      ?: return true

    val requiredRole = getRequiredRole(handler)
    if (!organizationRoleService.canUserView(userId, organization.id)) {
      // Security consideration: if the user cannot see the organization, pretend it does not exist.
      throw NotFoundException()
    }

    if (requiredRole != null && !organizationRoleService.isUserOfRole(userId, organization.id, requiredRole)) {
      throw PermissionException()
    }

    return true
  }

  private fun getRequiredRole(handler: HandlerMethod): OrganizationRoleType? {
    val defaultPerms = AnnotationUtils.getAnnotation(handler.method, UseDefaultPermissions::class.java)
    val orgPermission = AnnotationUtils.getAnnotation(handler.method, RequiresOrganizationRole::class.java)

    if (defaultPerms == null && orgPermission == null) {
      // A permission policy MUST be explicitly defined.
      throw RuntimeException("No permission policy have been set for this endpoint!")
    }

    if (defaultPerms != null && orgPermission != null) {
      // Policy doesn't make sense
      throw RuntimeException(
        "Both `@UseDefaultPermissions` and `@RequiresOrganizationRole` have been set for this endpoint!"
      )
    }

    return orgPermission?.role
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }

  companion object {
    val IS_SLUG_RE = "[a-z]".toRegex()
  }
}
