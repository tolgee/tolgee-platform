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
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.Scope
import io.tolgee.security.ProjectHolder
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.SecurityService
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * This interceptor performs authorization step to perform operations on a project or organization.
 */
@Component
class ProjectAuthorizationInterceptor(
  private val authenticationFacade: AuthenticationFacade,
  private val securityService: SecurityService,
  private val requestContextService: RequestContextService,
  private val projectHolder: ProjectHolder,
) : HandlerInterceptor, Ordered {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (handler !is HandlerMethod) {
      return super.preHandle(request, response, handler)
    }

    if (IS_GLOBAL_RE.matches(request.requestURI)) {
      // This is quite fragile and works on a known-ahead list.
      // Unfortunately there's not much better, since implicit projects are hard to distinguish from these routes.
      return true
    }

    val userId = authenticationFacade.authenticatedUser.id
    val project = requestContextService.getTargetProject(request)
      // Two possible scenarios: we're on a "global" route, or the project was not found.
      // In both cases, there is no authorization to perform and we simply continue.
      // It is not the job of the interceptor to return a 404 error.
      ?: return true

    projectHolder.project = project
    val requiredScopes = getRequiredScopes(request, handler)
    val scopes =
      if (authenticationFacade.isProjectApiKeyAuth)
        authenticationFacade.projectApiKey.scopesEnum.toTypedArray()
      else
        securityService.getProjectPermissionScopes(project.id, userId)

    if (scopes.isNullOrEmpty()) {
      // Security consideration: if the user cannot see the project, pretend it does not exist.
      throw NotFoundException()
    }

    requiredScopes?.forEach {
      if (!scopes.contains(it)) {
        throw PermissionException(
          Message.OPERATION_NOT_PERMITTED,
          requiredScopes.map { s -> s.value }
        )
      }
    }

    if (authenticationFacade.isProjectApiKeyAuth) {
      // Verify the key matches the project
      if (project.id != authenticationFacade.projectApiKey.project.id) {
        throw PermissionException()
      }

      // Validate scopes set on the key
      requiredScopes?.forEach {
        if (!authenticationFacade.projectApiKey.scopesEnum.contains(it)) {
          throw PermissionException(
            Message.OPERATION_NOT_PERMITTED,
            requiredScopes.map { s -> s.value }
          )
        }
      }
    }

    return true
  }

  private fun getRequiredScopes(request: HttpServletRequest, handler: HandlerMethod): Array<Scope>? {
    val defaultPerms = AnnotationUtils.getAnnotation(handler.method, UseDefaultPermissions::class.java)
    val projectPerms = AnnotationUtils.getAnnotation(handler.method, RequiresProjectPermissions::class.java)

    if (defaultPerms == null && projectPerms == null) {
      // A permission policy MUST be explicitly defined.
      throw RuntimeException("No permission policy have been set for URI ${request.requestURI}!")
    }

    if (defaultPerms != null && projectPerms != null) {
      // Policy doesn't make sense
      throw RuntimeException(
        "Both `@UseDefaultPermissions` and `@RequiresProjectPermissions` have been set for this endpoint!"
      )
    }

    if (projectPerms?.scopes?.isEmpty() == true) {
      // No scopes set for RequiresProjectPermissions
      throw RuntimeException(
        "`@RequiresProjectPermissions` requires at least one scope. Use `@UseDefaultPermissions` for any scope."
      )
    }

    return projectPerms?.scopes
  }

  override fun getOrder(): Int {
    return Ordered.HIGHEST_PRECEDENCE
  }

  companion object {
    // Excluded routes form filtering (exact):
    // - /v2/projects
    // - /v2/projects/
    // - /v2/projects/with-stats
    // - /v2/projects/with-stats/
    val IS_GLOBAL_RE = "^/v2/projects(?:/|/with-stats|/with-stats/)?$".toRegex()
  }
}
