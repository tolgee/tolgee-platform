package io.tolgee.mcp.tools.spec

import io.tolgee.dtos.cacheable.OrganizationDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CheckOrgRoleTest : McpToolEndpointSpecTestBase() {
  @Test
  fun `no required role skips check`() {
    sut.executeAs(spec(requiredOrgScopes = null)) {}

    verify(organizationRoleService, never()).getOrganizationScopes(any(), any())
  }

  @Test
  fun `no org in holder skips check`() {
    whenever(organizationHolder.organizationOrNull).thenReturn(null)

    sut.executeAs(spec(requiredOrgScopes = arrayOf(Scope.ORGANIZATION_SETTINGS_MANAGE))) {}

    verify(organizationRoleService, never()).getOrganizationScopes(any(), any())
  }

  @Test
  fun `user has required role passes`() {
    val orgDto = mock<OrganizationDto>()
    whenever(orgDto.id).thenReturn(42L)
    whenever(organizationHolder.organizationOrNull).thenReturn(orgDto)

    val userDto = mock<UserAccountDto>()
    whenever(userDto.id).thenReturn(7L)
    whenever(authenticationFacade.authenticatedUser).thenReturn(userDto)

    whenever(organizationRoleService.getOrganizationScopes(7L, 42L))
      .thenReturn(OrganizationRoleType.OWNER.availableScopes.toSet())

    sut.executeAs(spec(requiredOrgScopes = arrayOf(Scope.ORGANIZATION_SETTINGS_MANAGE))) {}
  }

  @Test
  fun `user lacks required role throws PermissionException`() {
    val orgDto = mock<OrganizationDto>()
    whenever(orgDto.id).thenReturn(42L)
    whenever(organizationHolder.organizationOrNull).thenReturn(orgDto)

    val userDto = mock<UserAccountDto>()
    whenever(userDto.id).thenReturn(7L)
    whenever(authenticationFacade.authenticatedUser).thenReturn(userDto)

    whenever(organizationRoleService.getOrganizationScopes(7L, 42L))
      .thenReturn(OrganizationRoleType.MEMBER.availableScopes.toSet())

    assertThatThrownBy {
      sut.executeAs(spec(requiredOrgScopes = arrayOf(Scope.ORGANIZATION_SETTINGS_MANAGE))) {}
    }.isInstanceOf(PermissionException::class.java)
  }
}
