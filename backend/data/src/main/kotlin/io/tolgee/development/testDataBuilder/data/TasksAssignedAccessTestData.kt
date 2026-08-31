package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TaskType
import io.tolgee.model.key.Key

/**
 * Two users with granular (non-preset) project permissions, assigned to the same task, differing only in
 * [Scope.TASKS_ASSIGNED_ACCESS].
 *
 * Kept apart from [TaskTestData] on purpose: several tests there assert exact task and assignee counts, so adding a
 * task or an assignee to it breaks them.
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

  lateinit var taskKey: Key

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
      // A second task, because the gates below ask whether *this* translation is in a task assigned to the caller,
      // and TASK_NUMBER's own task must stay empty for the finish-task case to remain finishable.
      val taskWithKey =
        addTask {
          number = TASK_WITH_KEY_NUMBER
          name = "Granular assignee task with a key"
          type = TaskType.TRANSLATE
          assignees = mutableSetOf(withScope, withoutScope)
          project = projectBuilder.self
          language = englishLanguage
          author = withScope
        }
      addKey {
        name = TASK_KEY_NAME
        taskKey = this
      }.build {
        addTranslation {
          language = englishLanguage
          text = "value"
        }
      }
      addTaskKey {
        this.task = taskWithKey.self
        key = taskKey
      }
    }
  }

  companion object {
    const val TASK_NUMBER = 1L
    const val TASK_WITH_KEY_NUMBER = 2L
    const val TASK_KEY_NAME = "granular-assignee-key"
  }
}
