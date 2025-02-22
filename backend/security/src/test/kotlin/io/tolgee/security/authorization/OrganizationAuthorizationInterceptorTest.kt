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
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.RequestContextService
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.organization.OrganizationRoleService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

class OrganizationAuthorizationInterceptorTest {
  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

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
    MockMvcBuilders.standaloneSetup(TestController::class.java)
      .addInterceptors(organizationAuthenticationInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(authenticationFacade.authentication).thenReturn(Mockito.mock(TolgeeAuthentication::class.java))
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(authenticationFacade.isApiAuthentication).thenReturn(false)
    Mockito.`when`(authenticationFacade.isUserSuperAuthenticated).thenReturn(false)

    Mockito.`when`(requestContextService.getTargetOrganization(any())).thenReturn(organization)

    Mockito.`when`(userAccount.id).thenReturn(1337L)
    Mockito.`when`(organization.id).thenReturn(1337L)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(
      authenticationFacade,
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
    Mockito.`when`(organizationRoleService.canUserViewStrict(1337L, 1337L))
      .thenReturn(false)

    mockMvc.perform(get("/v2/organizations/1337/default-perms")).andIsNotFound
    mockMvc.perform(get("/v2/organizations/1337/requires-admin")).andIsNotFound

    Mockito.`when`(organizationRoleService.canUserViewStrict(1337L, 1337L))
      .thenReturn(true)

    mockMvc.perform(get("/v2/organizations/1337/default-perms")).andIsOk
  }

  @Test
  fun `rejects access if the user does not have a sufficiently high role`() {
    Mockito.`when`(organizationRoleService.canUserViewStrict(1337L, 1337L))
      .thenReturn(true)
    Mockito.`when`(organizationRoleService.isUserOfRole(1337L, 1337L, OrganizationRoleType.OWNER))
      .thenReturn(false)

    mockMvc.perform(get("/v2/organizations/1337/requires-admin")).andIsForbidden

    Mockito.`when`(organizationRoleService.isUserOfRole(1337L, 1337L, OrganizationRoleType.OWNER))
      .thenReturn(true)

    mockMvc.perform(get("/v2/organizations/1337/requires-admin")).andIsOk
  }

  @RestController
  class TestController {
    @GetMapping("/v2/organizations")
    @IsGlobalRoute
    fun getAll() = "hello!"

    @GetMapping("/v2/organizations/{id}/not-annotated")
    fun notAnnotated(
      @PathVariable id: Long,
    ) = "henlo from org #$id!"

    @GetMapping("/v2/organizations/{id}/default-perms")
    @UseDefaultPermissions
    fun defaultPerms(
      @PathVariable id: Long,
    ) = "henlo from org #$id!"

    @GetMapping("/v2/organizations/{id}/requires-admin")
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    fun requiresAdmin(
      @PathVariable id: Long,
    ) = "henlo from org #$id!"

    @GetMapping("/v2/organizations/{id}/nonsense-perms")
    @RequiresOrganizationRole(OrganizationRoleType.OWNER)
    @UseDefaultPermissions
    fun nonsensePerms(
      @PathVariable id: Long,
    ) = "henlo from org #$id!"
  }
}
