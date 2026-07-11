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

import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.ReadOnlyOperation
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.security.authentication.WriteOperation
import io.tolgee.service.organization.OrganizationRoleService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

class OrganizationAuthorizationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val authentication = Mockito.mock(TolgeeAuthentication::class.java)

  private val organizationRoleService = Mockito.mock(OrganizationRoleService::class.java)

  private val requestContextService = Mockito.mock(RequestContextService::class.java)

  private val organization = Mockito.mock(OrganizationDto::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val organizationAuthenticationInterceptor =
    OrganizationAuthorizationInterceptor(
      authenticationFacade,
      organizationRoleService,
      requestContextService,
      Mockito.mock(OrganizationHolder::class.java),
    )

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(organizationAuthenticationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(authenticationFacade.authentication).thenReturn(authentication)
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(false)
    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(false)
    Mockito.`when`(authenticationFacade.isReadOnly).thenCallRealMethod()

    Mockito.`when`(authentication.isReadOnly).thenReturn(false)

    Mockito.`when`(requestContextService.getTargetOrganization(any())).thenReturn(organization)

    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.USER)
    Mockito.`when`(userAccount.id).thenReturn(1337L)
    Mockito.`when`(organization.id).thenReturn(1337L)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(
      authenticationFacade,
      authentication,
      organizationRoleService,
      requestContextService,
      organization,
      userAccount,
    )
  }

  @Test
  fun `it has no effect on endpoints not specific to a single organization`() {
    Mockito.`when`(requestContextService.getTargetOrganization(any())).thenReturn(null)
    mockMvc.perform(get("/v2/organizations")).andIsOk
  }

  @Test
  fun `it requires an annotation to be present on the handler`() {
    assertThrows<Exception> { mockMvc.perform(get("/v2/organizations/1337/not-annotated")) }
  }

  @Test
  fun `it does not allow both annotations to be present`() {
    assertThrows<Exception> { mockMvc.perform(get("/v2/organizations/1337/nonsense-perms")) }
  }

  @Test
  fun `it hides the organization if the user cannot see it`() {
    Mockito
      .`when`(organizationRoleService.canUserViewStrict(1337L, 1337L))
      .thenReturn(false)

    mockMvc.perform(get("/v2/organizations/1337/default-perms")).andIsNotFound
    mockMvc.perform(get("/v2/organizations/1337/requires-owner")).andIsNotFound

    Mockito
      .`when`(organizationRoleService.canUserViewStrict(1337L, 1337L))
      .thenReturn(true)

    mockMvc.perform(get("/v2/organizations/1337/default-perms")).andIsOk
  }

  @Test
  fun `rejects access if the user does not have a sufficiently high role`() {
    Mockito
      .`when`(organizationRoleService.canUserViewStrict(1337L, 1337L))
      .thenReturn(true)
    Mockito
      .`when`(organizationRoleService.isUserOfRole(1337L, 1337L, OrganizationRoleType.OWNER))
      .thenReturn(false)

    mockMvc.perform(get("/v2/organizations/1337/requires-owner")).andIsForbidden

    Mockito
      .`when`(organizationRoleService.isUserOfRole(1337L, 1337L, OrganizationRoleType.OWNER))
      .thenReturn(true)

    mockMvc.perform(get("/v2/organizations/1337/requires-owner")).andIsOk
  }

  @Test
  fun `it allows supporter to bypass checks for read-only organization endpoints`() {
    Mockito.`when`(organizationRoleService.canUserViewStrict(1337L, 1337L)).thenReturn(false)
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)

    performReadOnlyRequests { all -> all.andIsOk }
  }

  @Test
  fun `it does not let supporter bypass checks for write organization endpoints`() {
    Mockito.`when`(organizationRoleService.canUserViewStrict(1337L, 1337L)).thenReturn(false)
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.SUPPORTER)

    performWriteRequests { all -> all.andIsForbidden }
  }

  @Test
  fun `it allows admin to access any endpoint`() {
    Mockito.`when`(organizationRoleService.canUserViewStrict(1337L, 1337L)).thenReturn(false)
    Mockito.`when`(userAccount.role).thenReturn(UserAccount.Role.ADMIN)

    performReadOnlyRequests { all -> all.andIsOk }
    performWriteRequests { all -> all.andIsOk }
  }

  private fun performReadOnlyRequests(condition: (ResultActions) -> Unit) {
    // GET method
    mockMvc.perform(get("/v2/organizations/1337/default-perms")).andSatisfies(condition)
    mockMvc.perform(get("/v2/organizations/1337/requires-owner")).andSatisfies(condition)

    // POST method, but with read-only annotation
    mockMvc.perform(post("/v2/organizations/1337/requires-owner-read-annotation")).andSatisfies(condition)
  }

  private fun performWriteRequests(condition: (ResultActions) -> Unit) {
    // GET method, but with write annotation
    mockMvc.perform(get("/v2/organizations/1337/default-perms-write-annotation")).andSatisfies(condition)
    mockMvc.perform(get("/v2/organizations/1337/requires-owner-write-annotation")).andSatisfies(condition)

    // POST method
    mockMvc.perform(post("/v2/organizations/1337/default-perms-write-method")).andSatisfies(condition)
    mockMvc.perform(post("/v2/organizations/1337/requires-owner-write-method")).andSatisfies(condition)
  }

  private fun ResultActions.andSatisfies(condition: (ResultActions) -> Unit): ResultActions {
    condition(this)
    return this
  }

  @RestController
  class TestController {
    @GetMapping("/v2/organizations")
    @IsGlobalRoute
    fun getAll() = "hello!"

    @GetMapping("/v2/organizations/{id}/not-annotated")
    fun notAnnotated(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @GetMapping("/v2/organizations/{id}/default-perms")
    @UseDefaultPermissions
    fun defaultPerms(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @PostMapping("/v2/organizations/{id}/default-perms-write-method")
    @UseDefaultPermissions
    fun defaultPermsWriteMethod(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @GetMapping("/v2/organizations/{id}/default-perms-write-annotation")
    @UseDefaultPermissions
    @WriteOperation
    fun defaultPermsWriteAnnotation(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @GetMapping("/v2/organizations/{id}/requires-owner")
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    fun requiresOwner(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @PostMapping("/v2/organizations/{id}/requires-owner-write-method")
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    fun requiresOwnerWriteMethod(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @GetMapping("/v2/organizations/{id}/requires-owner-write-annotation")
    @WriteOperation
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    fun requiresOwnerWriteAnnotation(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @PostMapping("/v2/organizations/{id}/requires-owner-read-annotation")
    @ReadOnlyOperation
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    fun requiresOwnerReadAnnotation(
      @PathVariable id: Long,
    ) = "hello from org #$id!"

    @GetMapping("/v2/organizations/{id}/nonsense-perms")
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    @UseDefaultPermissions
    fun nonsensePerms(
      @PathVariable id: Long,
    ) = "hello from org #$id!"
  }
}
