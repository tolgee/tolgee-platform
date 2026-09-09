package io.tolgee.unit

import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.OrganizationRoleType.MAINTAINER
import io.tolgee.model.enums.OrganizationRoleType.MEMBER
import io.tolgee.model.enums.OrganizationRoleType.OWNER
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * Pins the organization role → scope mapping. If this changes, an org gate silently loosened or
 * tightened — update the endpoints and this test together, deliberately.
 */
class OrganizationRoleTypeScopesTest {
  @Test
  fun `member scopes`() {
    MEMBER.availableScopes.toSet().assert.containsExactlyInAnyOrder(
      Scope.ORGANIZATION_MEMBERS_VIEW,
      Scope.ORGANIZATION_USAGE_VIEW,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_VIEW,
    )
  }

  @Test
  fun `maintainer scopes`() {
    MAINTAINER.availableScopes.toSet().assert.containsExactlyInAnyOrder(
      Scope.ORGANIZATION_MEMBERS_VIEW,
      Scope.ORGANIZATION_USAGE_VIEW,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_VIEW,
      Scope.ORGANIZATION_PROJECTS_CREATE,
      Scope.ORGANIZATION_GLOSSARIES_MANAGE,
      Scope.ORGANIZATION_GLOSSARY_TERMS_MANAGE,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_MANAGE,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_ENTRIES_MANAGE,
    )
  }

  @Test
  fun `owner scopes`() {
    OWNER.availableScopes.toSet().assert.containsExactlyInAnyOrder(
      Scope.ORGANIZATION_MEMBERS_VIEW,
      Scope.ORGANIZATION_USAGE_VIEW,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_VIEW,
      Scope.ORGANIZATION_PROJECTS_CREATE,
      Scope.ORGANIZATION_GLOSSARIES_MANAGE,
      Scope.ORGANIZATION_GLOSSARY_TERMS_MANAGE,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_MANAGE,
      Scope.ORGANIZATION_TRANSLATION_MEMORY_ENTRIES_MANAGE,
      Scope.ORGANIZATION_MEMBERS_MANAGE,
      Scope.ORGANIZATION_SETTINGS_MANAGE,
      Scope.ORGANIZATION_DELETE,
      Scope.ORGANIZATION_APPS_MANAGE,
      Scope.ORGANIZATION_SLACK_MANAGE,
      Scope.ORGANIZATION_AI_MANAGE,
      Scope.ORGANIZATION_BILLING_VIEW,
      Scope.ORGANIZATION_BILLING_MANAGE,
    )
  }

  @Test
  fun `roles are nested owner superset of maintainer superset of member`() {
    OWNER.availableScopes
      .toSet()
      .assert
      .containsAll(MAINTAINER.availableScopes.toList())
    MAINTAINER.availableScopes
      .toSet()
      .assert
      .containsAll(MEMBER.availableScopes.toList())
    // strict: owner has more than maintainer, maintainer more than member
    OWNER.availableScopes.size.assert
      .isGreaterThan(MAINTAINER.availableScopes.size)
    MAINTAINER.availableScopes.size.assert
      .isGreaterThan(MEMBER.availableScopes.size)
  }

  @Test
  fun `every role scope is organization-level`() {
    OrganizationRoleType.entries.forEach { role ->
      role.availableScopes.forEach { scope ->
        scope.organizationLevel.assert.isTrue()
      }
    }
  }

  @Test
  fun `organization-level scopes are exactly those a role can grant`() {
    val grantable = OrganizationRoleType.entries.flatMap { it.availableScopes.toList() }.toSet()
    val orgScopes = Scope.entries.filter { it.organizationLevel }.toSet()
    grantable.assert.isEqualTo(orgScopes)
  }
}
