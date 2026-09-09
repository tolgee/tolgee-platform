package io.tolgee.unit

import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Guards the invariant that organization-level scopes can never become project permissions. If this
 * breaks, a project API key or granular project permission could carry an org scope.
 */
class OrganizationScopeIsolationTest {
  @Test
  fun `no project permission level grants an organization-level scope`() {
    ProjectPermissionType.entries.forEach { type ->
      type.availableScopes.forEach { scope ->
        scope.organizationLevel.assert.isFalse()
      }
    }
  }

  @Test
  fun `organization scopes and project-assignable scopes are disjoint`() {
    val projectAssignable =
      ProjectPermissionType.entries
        .flatMap { Scope.expand(it.availableScopes).toList() }
        .toSet()
    val orgScopes = Scope.entries.filter { it.organizationLevel }.toSet()
    projectAssignable.intersect(orgScopes).assert.isEmpty()
  }

  @Test
  fun `assertProjectAssignable rejects a set containing an organization scope`() {
    assertThrows<BadRequestException> {
      Scope.assertProjectAssignable(setOf(Scope.TRANSLATIONS_VIEW, Scope.ORGANIZATION_MEMBERS_MANAGE))
    }
  }

  @Test
  fun `assertProjectAssignable allows a set of project scopes`() {
    Scope.assertProjectAssignable(setOf(Scope.TRANSLATIONS_VIEW, Scope.KEYS_EDIT, Scope.ADMIN))
  }
}
