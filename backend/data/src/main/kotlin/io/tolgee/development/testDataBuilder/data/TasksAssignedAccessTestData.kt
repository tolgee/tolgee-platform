package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TaskType

/**
 * Two users with granular (non-preset) project permissions, assigned to the same task, differing only in
 * [Scope.TASKS_ASSIGNED_ACCESS].
 *
 * Kept apart from [TaskTestData] on purpose: several tests there assert exact task and assignee counts, so adding a
 * task or an assignee to it breaks them. The pair only means anything together — the role presets all grant the scope
 * and nothing backfills existing granular rows, so this is the one population whose behaviour changes on upgrade.
 */
class TasksAssignedAccessTestData : BaseTestData("tasksAssignedAccessOwner", "Tasks assigned-access project") {
  val withScope =
    root
      .addUserAccount {
        username = "granular.with.assigned.access@test.com"
      }.self

  val withoutScope =
    root
      .addUserAccount {
        username = "granular.without.assigned.access@test.com"
      }.self

  init {
    projectBuilder.apply {
      addPermission {
        user = withScope
        scopes = arrayOf(Scope.TRANSLATIONS_VIEW, Scope.TASKS_VIEW, Scope.TASKS_ASSIGNED_ACCESS)
      }
      addPermission {
        user = withoutScope
        scopes = arrayOf(Scope.TRANSLATIONS_VIEW, Scope.TASKS_VIEW)
      }
      addTask {
        number = TASK_NUMBER
        name = "Granular assignee task"
        type = TaskType.TRANSLATE
        assignees = mutableSetOf(withScope, withoutScope)
        project = projectBuilder.self
        language = englishLanguage
        author = withScope
      }
    }
  }

  companion object {
    const val TASK_NUMBER = 1L
  }
}
