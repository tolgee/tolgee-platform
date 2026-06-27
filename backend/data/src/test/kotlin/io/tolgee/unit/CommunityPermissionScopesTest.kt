package io.tolgee.unit

import io.tolgee.constants.ComputedPermissionOrigin
import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommunityPermissionScopesTest {
  @Test
  fun `community grant has the COMMUNITY origin`() {
    assertThat(ComputedPermissionDto.COMMUNITY.origin).isEqualTo(ComputedPermissionOrigin.COMMUNITY)
  }

  @Test
  fun `community raw scopes are exactly the expected set`() {
    assertThat(ComputedPermissionDto.COMMUNITY.scopes.toSet())
      .containsExactlyInAnyOrder(
        Scope.TRANSLATIONS_VIEW,
        Scope.SCREENSHOTS_VIEW,
        Scope.ACTIVITY_VIEW,
        Scope.TRANSLATIONS_SUGGEST,
        Scope.TRANSLATIONS_COMMENTS_ADD,
      )
  }

  @Test
  fun `community expanded scopes pull in the implied read scopes`() {
    assertThat(ComputedPermissionDto.COMMUNITY.expandedScopes.toSet())
      .contains(Scope.TRANSLATIONS_VIEW, Scope.KEYS_VIEW)
  }

  @Test
  fun `community never grants write, member, or restricted-internals scopes`() {
    assertThat(ComputedPermissionDto.COMMUNITY.expandedScopes.toSet())
      .doesNotContain(
        Scope.TRANSLATIONS_EDIT,
        Scope.TRANSLATIONS_STATE_EDIT,
        Scope.KEYS_EDIT,
        Scope.KEYS_CREATE,
        Scope.KEYS_DELETE,
        Scope.LANGUAGES_EDIT,
        Scope.MEMBERS_VIEW,
        Scope.MEMBERS_EDIT,
        Scope.BRANCH_MANAGEMENT,
        Scope.ORGANIZATION_QUOTAS_VIEW,
        Scope.PROMPTS_VIEW,
        Scope.ADMIN,
      )
  }

  @Test
  fun `community grant carries no language restriction (all languages)`() {
    val permission = ComputedPermissionDto.COMMUNITY
    assertThat(permission.viewLanguageIds).isNull()
    assertThat(permission.translateLanguageIds).isNull()
    assertThat(permission.suggestLanguageIds).isNull()
    assertThat(permission.stateChangeLanguageIds).isNull()
  }

  @Test
  fun `standard member permission types grant the organization-quotas scope`() {
    listOf(
      ProjectPermissionType.VIEW,
      ProjectPermissionType.TRANSLATE,
      ProjectPermissionType.REVIEW,
      ProjectPermissionType.EDIT,
      ProjectPermissionType.MANAGE,
    ).forEach { type ->
      assertThat(Scope.expand(type.availableScopes).toSet())
        .contains(Scope.ORGANIZATION_QUOTAS_VIEW)
    }
  }
}
