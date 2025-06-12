package io.tolgee.fixtures

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.unpack
import io.tolgee.testing.assertions.Assertions
import org.assertj.core.api.ObjectArrayAssert

fun ObjectArrayAssert<Scope>.equalsPermissionType(permissionType: ProjectPermissionType): ObjectArrayAssert<Scope>? {
  return satisfies({
    Assertions.assertThat(it.unpack()).containsExactlyInAnyOrder(*permissionType.availableScopes.unpack())
  })
}
