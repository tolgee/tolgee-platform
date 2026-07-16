package io.tolgee.api.v2.controllers

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.ComputedPermissionOrigin
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CommunityPermissionComputationTest : AbstractSpringTest() {
  @Test
  fun `org member with a NONE base is floored to community on a public project`() {
    val computed =
      permissionService.computeProjectPermission(
        organizationRole = OrganizationRoleType.MEMBER,
        organizationBasePermission = ComputedPermissionDto.NONE,
        directPermission = null,
        userRole = UserAccount.Role.USER,
        isProjectPublic = true,
      )
    assertThat(computed.origin).isEqualTo(ComputedPermissionOrigin.COMMUNITY)
    assertThat(computed.expandedScopes)
      .contains(Scope.TRANSLATIONS_SUGGEST, Scope.TRANSLATIONS_COMMENTS_ADD)
  }

  @Test
  fun `org member with a NONE base is NOT floored on a private project`() {
    val computed =
      permissionService.computeProjectPermission(
        organizationRole = OrganizationRoleType.MEMBER,
        organizationBasePermission = ComputedPermissionDto.NONE,
        directPermission = null,
        userRole = UserAccount.Role.USER,
        isProjectPublic = false,
      )
    assertThat(computed.expandedScopes)
      .doesNotContain(Scope.TRANSLATIONS_SUGGEST, Scope.TRANSLATIONS_COMMENTS_ADD)
  }

  @Test
  fun `a supporter non-member is never below a regular user on a public project`() {
    val computed =
      permissionService.computeProjectPermission(
        organizationRole = null,
        organizationBasePermission = ComputedPermissionDto.NONE,
        directPermission = null,
        userRole = UserAccount.Role.SUPPORTER,
        isProjectPublic = true,
      )
    assertThat(computed.expandedScopes).contains(
      Scope.TRANSLATIONS_SUGGEST,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.MEMBERS_VIEW,
      Scope.TASKS_VIEW,
    )
  }

  @Test
  fun `a server admin still gets full admin on a public project`() {
    val computed =
      permissionService.computeProjectPermission(
        organizationRole = null,
        organizationBasePermission = ComputedPermissionDto.NONE,
        directPermission = null,
        userRole = UserAccount.Role.ADMIN,
        isProjectPublic = true,
      )
    assertThat(computed.origin).isEqualTo(ComputedPermissionOrigin.SERVER_ADMIN)
    assertThat(computed.expandedScopes).contains(Scope.ADMIN)
  }

  @Test
  fun `community floor lifts view and suggest language restrictions but keeps translate restricted`() {
    val restrictedToOneLanguage =
      object : IPermission {
        override val scopes =
          arrayOf(
            Scope.TRANSLATIONS_VIEW,
            Scope.SCREENSHOTS_VIEW,
            Scope.ACTIVITY_VIEW,
            Scope.TRANSLATIONS_SUGGEST,
            Scope.TRANSLATIONS_COMMENTS_ADD,
            Scope.TRANSLATIONS_EDIT,
          )
        override val projectId: Long? = null
        override val organizationId: Long? = null
        override val viewLanguageIds = setOf(1L)
        override val translateLanguageIds = setOf(1L)
        override val stateChangeLanguageIds = setOf(1L)
        override val suggestLanguageIds = setOf(1L)
        override val suggestManageLanguageIds = setOf(1L)
        override val type = ProjectPermissionType.EDIT
        override val granular = true
      }
    val computed =
      permissionService.computeProjectPermission(
        organizationRole = null,
        organizationBasePermission = ComputedPermissionDto.NONE,
        directPermission = restrictedToOneLanguage,
        userRole = UserAccount.Role.USER,
        isProjectPublic = true,
      )
    assertThat(computed.viewLanguageIds).isNull()
    assertThat(computed.suggestLanguageIds).isNull()
    assertThat(computed.translateLanguageIds).containsExactly(1L)
  }

  @Test
  fun `public project grants nothing without an authenticated user`() {
    val computed =
      permissionService.computeProjectPermission(
        organizationRole = null,
        organizationBasePermission = ComputedPermissionDto.NONE,
        directPermission = null,
        userRole = null,
        isProjectPublic = true,
      )
    assertThat(computed.expandedScopes).isEmpty()
    assertThat(computed.origin).isNotEqualTo(ComputedPermissionOrigin.COMMUNITY)
  }
}
