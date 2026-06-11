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

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
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
    if (authenticationFacade.isAppAuth) {
      // Apps are rejected from org-level endpoints unless the endpoint explicitly opts in via
      // @AllowAppAccessWithOrgScope and the app's install (in this org) holds the required scope.
      return handleAppAuthorization(request, handler)
    }

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

  /**
   * Authorizes an app token on an org-level endpoint. Allowed only when the endpoint opts in via
   * [AllowAppAccessWithOrgScope], the app's install belongs to the target organization, and the
   * install's granted scopes include the required org-level scope(s). The org owner consented to
   * those scopes at install time, so no user organization role is checked (install grant suffices).
   */
  private fun handleAppAuthorization(
    request: HttpServletRequest,
    handler: HandlerMethod,
  ): Boolean {
    val annotation =
      AnnotationUtils.getAnnotation(handler.method, AllowAppAccessWithOrgScope::class.java)
        ?: throw PermissionException(Message.APP_TOKEN_NOT_ALLOWED_FOR_ENDPOINT)

    val organization =
      requestContextService.getTargetOrganization(request)
        ?: throw PermissionException(Message.APP_TOKEN_NOT_ALLOWED_FOR_ENDPOINT)

    val appInstall = authenticationFacade.appAuthentication.appInstall
    if (appInstall.organization.id != organization.id) {
      // An app may only act on its own organization.
      throw PermissionException(Message.APP_TOKEN_NOT_ALLOWED_FOR_ENDPOINT)
    }

    val granted = Scope.expand(appInstall.grantedScopes).toSet()
    val missing = annotation.scopes.toList().filterNot { granted.contains(it) }
    if (missing.isNotEmpty()) {
      throw PermissionException(missing)
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
