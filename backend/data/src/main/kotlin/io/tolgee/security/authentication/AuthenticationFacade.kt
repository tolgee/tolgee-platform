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

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.*
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.*
import io.tolgee.security.ProjectNotSelectedException
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationFacade(
  private val userAccountService: UserAccountService,
  private val projectService: ProjectService,
  private val organizationService: OrganizationService,
) {
  // -- GENERAL AUTHENTICATION INFO
  val isAuthenticated: Boolean
    get() = SecurityContextHolder.getContext().authentication is TolgeeAuthentication

  val authentication: TolgeeAuthentication
    get() = SecurityContextHolder.getContext().authentication as? TolgeeAuthentication
      ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  // -- CURRENT USER
  val authenticatedUser: UserAccountDto
    get() = authentication.principal

  val authenticatedUserOrNull: UserAccountDto?
    get() = if (isAuthenticated) authentication.principal else null

  val authenticatedUserEntity: UserAccount
    get() = authenticatedUserEntityOrNull ?: throw AuthenticationException(Message.UNAUTHENTICATED)

  val authenticatedUserEntityOrNull: UserAccount?
    get() = if (isAuthenticated) userAccountService.findActive(authenticatedUser.id) else null

  // -- CURRENT ORGANIZATION
  val targetOrganization: OrganizationDto
    get() = targetOrganizationOrNull ?: throw IllegalStateException("No available organization")

  val targetOrganizationOrNull: OrganizationDto?
    get() = if (isAuthenticated) authentication.targetOrganization else null

  val targetOrganizationEntity: Organization
    get() = targetOrganizationEntityOrNull ?: throw IllegalStateException("No available organization")

  val targetOrganizationEntityOrNull: Organization?
    get() = targetOrganizationOrNull?.let { organizationService.find(it.id) }

  // -- CURRENT PROJECT
  val targetProject: ProjectDto
    get() = targetProjectOrNull ?: throw ProjectNotSelectedException()

  val targetProjectOrNull: ProjectDto?
    get() = if (isAuthenticated) authentication.targetProject else null

  val targetProjectEntity: Project
    get() = targetProjectEntityOrNull ?: throw ProjectNotSelectedException()

  val targetProjectEntityOrNull: Project?
    get() = targetProjectOrNull?.let { projectService.find(it.id) }

  // -- AUTHENTICATION METHOD AND DETAILS
  val isUserSuperAuthenticated: Boolean
    get() = if (isAuthenticated) authentication.details?.isSuperToken == true else false

  val isApiAuthentication: Boolean
    get() =
      if (isAuthenticated)
        authentication.credentials is ApiKeyDto || authentication.credentials is PatDto
      else
        false

  val isProjectApiKeyAuth: Boolean
    get() = if (isAuthenticated) authentication.credentials is ApiKeyDto else false

  val isPersonalAccessTokenAuth: Boolean
    get() = if (isAuthenticated) authentication.credentials is PatDto else false

  val projectApiKey: ApiKeyDto
    get() = authentication.credentials as ApiKeyDto

  val personalAccessToken: PatDto
    get() = authentication.credentials as PatDto
}
