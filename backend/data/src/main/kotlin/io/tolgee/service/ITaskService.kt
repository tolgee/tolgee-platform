package io.tolgee.service

import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.model.views.TranslationToTaskView

interface ITaskService {
  fun deleteAll(tasks: List<Task>)

  fun findAssigneeById(
    projectId: Long,
    taskNumber: Long,
    userId: Long,
  ): List<UserAccount>

  fun findAssigneeByKey(
    keyId: Long,
    languageId: Long,
    userId: Long,
    type: TaskType? = null,
  ): List<UserAccount>

  fun getKeysWithTasks(
    userId: Long,
    keyIds: Collection<Long>,
  ): Map<Long, List<TranslationToTaskView>>
}
