package io.tolgee.unit

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * [Scope.TASKS_ASSIGNED_ACCESS] gates the task-assignee elevation. Its placement in the hierarchy is load-bearing in
 * two ways that are easy to undo by accident, so both are pinned here.
 */
class TasksAssignedAccessScopeTest {
  @Test
  fun `is not a read-only scope`() {
    assertThat(Scope.readOnlyScopes).doesNotContain(Scope.TASKS_ASSIGNED_ACCESS)
    assertThat(Scope.TASKS_ASSIGNED_ACCESS.isReadOnly()).isFalse()
  }

  @Test
  fun `expands to itself only`() {
    assertThat(Scope.TASKS_ASSIGNED_ACCESS.expand()).containsExactly(Scope.TASKS_ASSIGNED_ACCESS)
  }

  @Test
  fun `is granted by every role preset that can be assigned to a task`() {
    val roles = ProjectPermissionType.getRoles()
    assertThat(roles[ProjectPermissionType.VIEW.name]).contains(Scope.TASKS_ASSIGNED_ACCESS)
    assertThat(roles[ProjectPermissionType.TRANSLATE.name]).contains(Scope.TASKS_ASSIGNED_ACCESS)
    assertThat(roles[ProjectPermissionType.REVIEW.name]).contains(Scope.TASKS_ASSIGNED_ACCESS)
    assertThat(roles[ProjectPermissionType.EDIT.name]).contains(Scope.TASKS_ASSIGNED_ACCESS)
    assertThat(roles[ProjectPermissionType.MANAGE.name]).contains(Scope.TASKS_ASSIGNED_ACCESS)
  }

  @Test
  fun `is not granted by the NONE preset`() {
    assertThat(ProjectPermissionType.getRoles()[ProjectPermissionType.NONE.name])
      .doesNotContain(Scope.TASKS_ASSIGNED_ACCESS)
  }

  @Test
  fun `is reachable from ADMIN`() {
    assertThat(Scope.ADMIN.expand()).contains(Scope.TASKS_ASSIGNED_ACCESS)
  }
}
