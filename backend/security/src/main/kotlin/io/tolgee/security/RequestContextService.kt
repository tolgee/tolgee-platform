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

package io.tolgee.security

import io.tolgee.model.Organization
import io.tolgee.model.Project
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authorization.OrganizationAuthorizationInterceptor
import io.tolgee.security.project_auth.ProjectNotSelectedException
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Service
class RequestContextService(
  private val authenticationFacade: AuthenticationFacade,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
) {
  /**
   * Detects and returns the target project for the current request.
   * Resolves the "implicit project" from the Project API Key if necessary.
   * **Must be called on relevant endpoints, or invalid data may be returned!**
   *
   * @return The project, or null if the request does not target a specific project.
   * @throws ProjectNotSelectedException The request uses implicit project but didn't use a PAK.
   */
  fun getTargetProject(request: HttpServletRequest): Project? {
    val matchedPath = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
    if (matchedPath.contains(IS_EXPLICIT_RE)) {
      return getTargetProjectExplicit(request)
    }

    return getTargetProjectImplicit()
  }

  private fun getTargetProjectExplicit(request: HttpServletRequest): Project? {
    val pathVariablesMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
    if (pathVariablesMap.isEmpty()) {
      // This is possible if we're creating a project or listing all visible projects.
      // No permission check needs to occur here.
      return null
    }

    val id = (pathVariablesMap.values.first() as String).toLong()
    return projectService.find(id)
  }

  private fun getTargetProjectImplicit(): Project {
    // This method is the source of complexity for the global handling, but is itself quite simple. Oh, the irony!
    if (!authenticationFacade.isProjectApiKeyAuth) {
      throw ProjectNotSelectedException()
    }

    return authenticationFacade.projectApiKey.project
  }

  /**
   * Detects and returns the target organization for the current request.
   * **Must be called on relevant endpoints, or invalid data may be returned!**
   *
   * @return The organization, or null if the request does not target a specific organization.
   */
  fun getTargetOrganization(request: HttpServletRequest): Organization? {
    val pathVariablesMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
    if (pathVariablesMap.isEmpty()) {
      // This is possible if we're creating an org or listing all visible organizations.
      // No permission check needs to occur here.
      return null
    }

    val idOrSlug = pathVariablesMap.values.first() as String
    if (idOrSlug.contains(OrganizationAuthorizationInterceptor.IS_SLUG_RE)) {
      return organizationService.find(idOrSlug)
    }

    return organizationService.find(idOrSlug.toLong())
  }

  companion object {
    val IS_EXPLICIT_RE = "^/(?:api/(?:project|repository)|v2/projects)/\\{".toRegex()
  }
}
