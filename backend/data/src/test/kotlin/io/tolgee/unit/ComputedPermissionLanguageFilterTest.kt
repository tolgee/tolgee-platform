package io.tolgee.unit

import io.tolgee.dtos.ComputedPermissionDto
import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ComputedPermissionLanguageFilterTest {
  private fun language(id: Long) = Language().apply { this.id = id }

  @Test
  fun `empty language set means all languages`() {
    val dto = ComputedPermissionDto(Permission(type = ProjectPermissionType.REVIEW))
    dto.filterStateChangePermitted(listOf(1L, 2L)).assert.containsExactly(1L, 2L)
  }

  @Test
  fun `restricted set keeps only permitted languages`() {
    val permission =
      Permission(type = ProjectPermissionType.REVIEW).apply {
        stateChangeLanguages = mutableSetOf(language(2L))
        viewLanguages = mutableSetOf(language(2L))
      }
    val dto = ComputedPermissionDto(permission)
    dto.filterStateChangePermitted(listOf(1L, 2L)).assert.containsExactly(2L)
    dto.filterViewPermitted(listOf(1L, 2L, 3L)).assert.containsExactly(2L)
  }

  @Test
  fun `no scopes means no languages`() {
    val dto = ComputedPermissionDto(Permission(type = null).apply { scopes = arrayOf() })
    dto.filterViewPermitted(listOf(1L)).assert.isEmpty()
  }

  @Test
  fun `admin scope means all languages`() {
    val permission =
      Permission(type = null).apply {
        scopes = arrayOf(Scope.ADMIN)
        viewLanguages = mutableSetOf(language(2L))
      }
    ComputedPermissionDto(permission).filterViewPermitted(listOf(1L, 2L)).assert.containsExactly(1L, 2L)
  }
}
