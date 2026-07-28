package io.tolgee.unit

import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class OrganizationRoleTypeTest {
  @Test
  fun `declaration order is pinned because the entity persists the type by ordinal`() {
    OrganizationRoleType.entries
      .map { it.name }
      .assert
      .containsExactly("MEMBER", "OWNER", "MAINTAINER")
  }
}
