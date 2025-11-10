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
import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.ProjectHolder
import io.tolgee.security.ProjectNotSelectedException
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.security.authentication.WriteOperation
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.SecurityService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

class ProjectAuthorizationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val authentication = Mockito.mock(TolgeeAuthentication::class.java)

  private val organizationService = Mockito.mock(OrganizationService::class.java)

  private val securityService = Mockito.mock(SecurityService::class.java)

  private val requestContextService = Mockito.mock(RequestContextService::class.java)

  private val project = Mockito.mock(Project::class.java)

  private val projectDto = Mockito.mock(ProjectDto::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val apiKey = Mockito.mock(ApiKeyDto::class.java)

  private val projectAuthenticationInterceptor =
    ProjectAuthorizationInterceptor(
      authenticationFacade,
      organizationService,
      securityService,
      requestContextService,
      Mockito.mock(ProjectHolder::class.java),
      Mockito.mock(OrganizationHolder::class.java),
      Mockito.mock(ActivityHolder::class.java, Mockito.RETURNS_DEEP_STUBS),
    )

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(projectAuthenticationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(authenticationFacade.authentication).thenReturn(authentication)
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(false)
    Mockito.`when`(authenticationFacade.isProjectApiKeyAuth).thenReturn(false)
    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(false)
    Mockito.`when`(authenticationFacade.projectApiKey).thenReturn(apiKey)
    Mockito.`when`(authenticationFacade.isReadOnly).thenCallRealMethod()

    Mockito.`when`(authentication.isReadOnly).thenReturn(false)

    val mockOrganizationDto = Mockito.mock(OrganizationDto::class.java)
    Mockito.`when`(organizationService.findDto(Mockito.anyLong())).thenReturn(mockOrganizationDto)
    Mockito.`when`(requestContextService.getTargetProject(any())).thenReturn(projectDto)

    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.USER)
    Mockito.`when`(userAccount.id).thenReturn(1337L)
    Mockito.`when`(projectDto.id).thenReturn(1337L)
    Mockito.`when`(project.id).thenReturn(1337L)

    Mockito.`when`(apiKey.projectId).thenReturn(1337L)
    Mockito.`when`(apiKey.scopes).thenReturn(mutableSetOf(Scope.KEYS_CREATE))
    Mockito.`when`(securityService.getCurrentPermittedScopes(1337L)).thenReturn(mutableSetOf(Scope.KEYS_CREATE))
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(
      authenticationFacade,
      authentication,
      organizationService,
      securityService,
      requestContextService,
      project,
      projectDto,
      userAccount,
      apiKey,
    )
  }

  @Test
  fun `it has no effect on endpoints not specific to a single project`() {
    Mockito.`when`(requestContextService.getTargetOrganization(any())).thenReturn(null)
    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects")).andIsOk
  }

  @Test
  fun `it requires an annotation to be present on the handler`() {
    assertThrows<Exception> { mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/not-annotated")) }
  }

  @Test
  fun `it does not allow both annotations to be present`() {
    assertThrows<Exception> { mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/nonsense-perms")) }
  }

  @Test
  fun `it hides the organization if the user cannot see it`() {
    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(emptySet())

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/default-perms")).andIsNotFound
    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-admin")).andIsNotFound

    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_VIEW))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/default-perms")).andIsOk
  }

  @Test
  fun `rejects access if the user does not have the required scope (single scope)`() {
    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_VIEW))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andIsForbidden

    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_CREATE))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andIsOk
  }

  @Test
  fun `rejects access if the user is admin and authorizes with API key`() {
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(false)
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.ADMIN)

    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_VIEW))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andIsOk

    Mockito.`when`(authenticationFacade.isProjectApiKeyAuth).thenReturn(true)
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.ADMIN)

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andIsForbidden
  }

  @Test
  fun `rejects access if the user does not have the required scope (multiple scopes)`() {
    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_CREATE))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-multiple-scopes")).andIsForbidden

    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.MEMBERS_EDIT))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-multiple-scopes")).andIsForbidden

    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_CREATE, Scope.MEMBERS_EDIT))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-multiple-scopes")).andIsOk
  }

  @Test
  fun `ensures API key works only for the project it is bound to`() {
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(true)
    Mockito.`when`(authenticationFacade.isProjectApiKeyAuth).thenReturn(true)

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andIsOk

    val fakeProject = Mockito.mock(Project::class.java)
    Mockito.`when`(fakeProject.id).thenReturn(7331L)
    Mockito.`when`(apiKey.projectId).thenReturn(7331L)

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andIsForbidden
  }

  @Test
  fun `it restricts scopes (multiple scopes)`() {
    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-multiple-scopes")).andIsForbidden

    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_CREATE, Scope.MEMBERS_EDIT))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-multiple-scopes")).andIsOk
  }

  @Test
  fun `it does not let scopes on the key work if the authenticated user does not have them`() {
    Mockito.`when`(apiKey.scopes).thenReturn(mutableSetOf(Scope.KEYS_CREATE, Scope.MEMBERS_EDIT))
    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_CREATE))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-multiple-scopes")).andIsForbidden
  }

  @Test
  fun `permissions work as intended when using implicit project id`() {
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(true)
    Mockito.`when`(authenticationFacade.isProjectApiKeyAuth).thenReturn(true)
    Mockito.`when`(securityService.getCurrentPermittedScopes(1337L)).thenReturn(setOf(Scope.KEYS_CREATE))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/implicit-access")).andIsOk

    Mockito.`when`(apiKey.scopes).thenReturn(mutableSetOf(Scope.KEYS_VIEW))
    Mockito
      .`when`(securityService.getCurrentPermittedScopes(1337L))
      .thenReturn(setOf(Scope.KEYS_VIEW))

    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/implicit-access")).andIsForbidden

    Mockito.`when`(requestContextService.getTargetProject(any())).thenThrow(ProjectNotSelectedException())

    assertThrows<Exception> { mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/implicit-access")) }
  }

  @Test
  fun `it allows supporter to bypass checks for read-only project endpoints`() {
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    Mockito.`when`(securityService.getCurrentPermittedScopes(1337L)).thenReturn(emptySet())

    performReadOnlyRequests { all -> all.andIsOk }
  }

  @Test
  fun `it does not let supporter to bypass checks for write project endpoints`() {
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)
    Mockito.`when`(securityService.getCurrentPermittedScopes(1337L)).thenReturn(emptySet())

    performWriteRequests { all -> all.andIsForbidden }
  }

  @Test
  fun `it allows admin to access any endpoint`() {
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.ADMIN)
    Mockito.`when`(securityService.getCurrentPermittedScopes(1337L)).thenReturn(emptySet())

    performReadOnlyRequests { all -> all.andIsOk }
    performWriteRequests { all -> all.andIsOk }
  }

  private fun performReadOnlyRequests(condition: (ResultActions) -> Unit) {
    // GET method
    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/default-perms")).andSatisfies(condition)
    mockMvc.perform(MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope")).andSatisfies(condition)

    // POST method, but with read-only annotation
    mockMvc
      .perform(
        MockMvcRequestBuilders.post("/v2/projects/1337/requires-single-scope-read-annotation"),
      ).andSatisfies(condition)
  }

  private fun performWriteRequests(condition: (ResultActions) -> Unit) {
    // GET method, but with write annotation
    mockMvc
      .perform(
        MockMvcRequestBuilders.get("/v2/projects/1337/default-perms-write-annotation"),
      ).andSatisfies(condition)
    mockMvc
      .perform(
        MockMvcRequestBuilders.get("/v2/projects/1337/requires-single-scope-write-annotation"),
      ).andSatisfies(condition)

    // POST method
    mockMvc.perform(MockMvcRequestBuilders.post("/v2/projects/1337/default-perms-write-method")).andSatisfies(condition)
    mockMvc
      .perform(
        MockMvcRequestBuilders.post("/v2/projects/1337/requires-single-scope-write-method"),
      ).andSatisfies(condition)
  }

  private fun ResultActions.andSatisfies(condition: (ResultActions) -> Unit): ResultActions {
    condition(this)
    return this
  }

  @RestController
  class TestController {
    @GetMapping("/v2/projects")
    @IsGlobalRoute
    fun getAll() = "hello!"

    @GetMapping("/v2/projects/{id}/not-annotated")
    fun notAnnotated(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/{id}/default-perms")
    @UseDefaultPermissions
    fun defaultPerms(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/{id}/default-perms-write-annotation")
    @UseDefaultPermissions
    @WriteOperation
    fun defaultPermsWriteAnnotation(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @PostMapping("/v2/projects/{id}/default-perms-write-method")
    @UseDefaultPermissions
    fun defaultPermsWriteMethod(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/{id}/requires-single-scope")
    @RequiresProjectPermissions([Scope.KEYS_CREATE])
    fun requiresSingleScope(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/{id}/requires-single-scope-write-annotation")
    @WriteOperation
    @RequiresProjectPermissions([Scope.KEYS_CREATE])
    fun requiresSingleScopeWriteAnnotation(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @PostMapping("/v2/projects/{id}/requires-single-scope-write-method")
    @RequiresProjectPermissions([Scope.KEYS_CREATE])
    fun requiresSingleScopeWriteMethod(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @PostMapping("/v2/projects/{id}/requires-single-scope-read-annotation")
    @ReadOnlyOperation
    @RequiresProjectPermissions([Scope.KEYS_CREATE])
    fun requiresSingleScopeReadAnnotation(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/{id}/requires-multiple-scopes")
    @RequiresProjectPermissions([Scope.KEYS_CREATE, Scope.MEMBERS_EDIT])
    fun requiresMultipleScopes(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/{id}/nonsense-perms")
    @UseDefaultPermissions
    @RequiresProjectPermissions([Scope.PROJECT_EDIT])
    fun nonsensePerms(
      @PathVariable id: Long,
    ) = "hello from project $id!"

    @GetMapping("/v2/projects/implicit-access")
    @RequiresProjectPermissions([Scope.KEYS_CREATE])
    fun implicitProject() = "hello from implicit project!"
  }
}
