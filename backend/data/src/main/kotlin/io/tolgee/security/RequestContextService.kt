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

import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.InvalidPathException
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.web.servlet.HandlerMapping

@Service
class RequestContextService(
  private val authenticationFacade: AuthenticationFacade,
  @Lazy
  private val projectService: ProjectService,
  @Lazy
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
  fun getTargetProject(request: HttpServletRequest): ProjectDto? {
    val matchedPath = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
    if (matchedPath.contains(IS_EXPLICIT_RE)) {
      return getTargetProjectExplicit(request)
    }

    return getTargetProjectImplicit()
  }

  private fun getTargetProjectExplicit(request: HttpServletRequest): ProjectDto? {
    val pathVariablesMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
    if (pathVariablesMap.isEmpty()) {
      // This is possible if we're creating a project or listing all visible projects.
      // No permission check needs to occur here.
      return null
    }
    val idAsString = pathVariablesMap.values.first() as String
    return runCatching { projectService.findDto(idAsString.toLong()) }
      .onFailure { throw InvalidPathException("Invalid format of project id: $idAsString") }
      .getOrNull()
  }

  private fun getTargetProjectImplicit(): ProjectDto? {
    // This method is the source of complexity for the global handling, but is itself quite simple. Oh, the irony!
    if (!authenticationFacade.isProjectApiKeyAuth) {
      throw ProjectNotSelectedException()
    }

    return projectService.findDto(authenticationFacade.projectApiKey.projectId)
  }

  /**
   * Detects and returns the target organization for the current request.
   * **Must be called on relevant endpoints, or invalid data may be returned!**
   *
   * @return The organization, or null if the request does not target a specific organization.
   */
  fun getTargetOrganization(request: HttpServletRequest): OrganizationDto? {
    val pathVariablesMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
    if (pathVariablesMap.isEmpty()) {
      // This is possible if we're creating an org or listing all visible organizations.
      // No permission check needs to occur here.
      return null
    }

    val idOrSlug = pathVariablesMap.values.first() as String
    if (idOrSlug.contains(IS_SLUG_RE)) {
      return organizationService.findDto(idOrSlug)
    }
    return runCatching { organizationService.findDto(idOrSlug.toLong()) }
      .onFailure { throw InvalidPathException("Invalid format of organization id: $idOrSlug") }
      .getOrNull()
  }

  companion object {
    val IS_EXPLICIT_RE = "^/(?:api/(?:project|repository)|v2/projects)/\\{".toRegex()
    val IS_SLUG_RE = "[a-z]".toRegex()
  }
}
