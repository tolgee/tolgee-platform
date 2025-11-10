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

import io.tolgee.activity.ActivityHolder
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.cacheable.isSupporterOrAdmin
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.exceptions.ProjectNotFoundException
import io.tolgee.model.enums.Scope
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectHolder
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.isReadOnly
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.SecurityService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * This interceptor performs an authorization step to perform operations on a project or organization.
 */
@Component
class ProjectAuthorizationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  private val organizationService: OrganizationService,
  private val securityService: SecurityService,
  private val requestContextService: RequestContextService,
  private val projectHolder: ProjectHolder,
  private val organizationHolder: OrganizationHolder,
  private val activityHolder: ActivityHolder,
) : AbstractAuthorizationInterceptor() {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun preHandleInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    handler: HandlerMethod,
  ): Boolean {
    val userId = authenticationFacade.authenticatedUser.id
    val project =
      requestContextService.getTargetProject(request)
        // Two possible scenarios: we're on a "global" route, or the project was not found.
        // In both cases, there is no authorization to perform, and we simply continue.
        // It is not the job of the interceptor to return a 404 error.
        ?: return true

    var bypassed = false
    val requiredScopes = getRequiredScopes(request, handler)

    val formattedRequirements = requiredScopes?.joinToString(", ") { it.value } ?: "read-only"
    logger.debug("Checking access to proj#${project.id} by user#$userId (Requires $formattedRequirements)")

    val scopes = securityService.getCurrentPermittedScopes(project.id)

    if (scopes.isEmpty()) {
      if (!canBypass(request, handler)) {
        logger.debug(
          "Rejecting access to proj#{} for user#{} - No view permissions",
          project.id,
          userId,
        )

        if (!canBypassForReadOnly()) {
          // Security consideration: if the user cannot see the project, pretend it does not exist.
          throw ProjectNotFoundException(project.id)
        }

        // Admin access for read-only operations is allowed, but it's not enough for the current operation.
        throw PermissionException()
      }

      bypassed = true
    }

    val missingScopes = getMissingScopes(requiredScopes, scopes)

    if (missingScopes.isNotEmpty()) {
      if (!canBypass(request, handler)) {
        logger.debug(
          "Rejecting access to proj#{} for user#{} - Insufficient permissions",
          project.id,
          userId,
        )

        throw PermissionException(
          Message.OPERATION_NOT_PERMITTED,
          missingScopes.map { it.value },
        )
      }

      bypassed = true
    }

    if (authenticationFacade.isProjectApiKeyAuth) {
      val pak = authenticationFacade.projectApiKey
      // Verify the key matches the project
      if (project.id != pak.projectId) {
        logger.debug(
          "Rejecting access to proj#{} for user#{} via pak#{} - API Key mismatch",
          project.id,
          userId,
          pak.id,
        )

        throw PermissionException(Message.PAK_CREATED_FOR_DIFFERENT_PROJECT)
      }
    }

    if (bypassed) {
      logger.info(
        "Use of admin privileges: user#{} failed local security checks for proj#{} - bypassing for {} {}",
        userId,
        project.id,
        request.method,
        request.requestURI,
      )
    }

    projectHolder.project = project
    activityHolder.activityRevision.projectId = project.id
    organizationHolder.organization = organizationService.findDto(project.organizationOwnerId)
      ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)

    return true
  }

  private fun getMissingScopes(
    requiredScopes: Array<Scope>?,
    permittedScopes: Collection<Scope>?,
  ): Set<Scope> {
    val permitted = permittedScopes?.toSet() ?: setOf()
    val required = requiredScopes?.toSet() ?: setOf()
    return required - permitted
  }

  private fun getRequiredScopes(
    request: HttpServletRequest,
    handler: HandlerMethod,
  ): Array<Scope>? {
    val defaultPerms = AnnotationUtils.getAnnotation(handler.method, UseDefaultPermissions::class.java)
    val projectPerms = AnnotationUtils.getAnnotation(handler.method, RequiresProjectPermissions::class.java)

    if (defaultPerms == null && projectPerms == null) {
      // A permission policy MUST be explicitly defined.
      throw RuntimeException("No permission policy have been set for URI ${request.requestURI}!")
    }

    if (defaultPerms != null && projectPerms != null) {
      // Policy doesn't make sense
      throw RuntimeException(
        "Both `@UseDefaultPermissions` and `@RequiresProjectPermissions` have been set for this endpoint!",
      )
    }

    if (projectPerms?.scopes?.isEmpty() == true) {
      // No scopes set for RequiresProjectPermissions
      throw RuntimeException(
        "`@RequiresProjectPermissions` requires at least one scope. Use `@UseDefaultPermissions` for any scope.",
      )
    }

    return projectPerms?.scopes
  }

  private val canUseAdminPermissions
    get() = !authenticationFacade.isProjectApiKeyAuth

  private fun canBypass(
    request: HttpServletRequest,
    handler: HandlerMethod,
  ): Boolean {
    if (!canUseAdminPermissions) {
      return false
    }

    if (authenticationFacade.authenticatedUser.isAdmin()) {
      return true
    }

    val forReadOnly = handler.isReadOnly(request.method)
    return forReadOnly && canBypassForReadOnly()
  }

  private fun canBypassForReadOnly(): Boolean {
    return canUseAdminPermissions && authenticationFacade.authenticatedUser.isSupporterOrAdmin()
  }
}
