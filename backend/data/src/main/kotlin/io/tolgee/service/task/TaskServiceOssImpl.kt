package io.tolgee.service.task

import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.model.views.TranslationToTaskView
import org.springframework.stereotype.Component

@Component
class TaskServiceOssImpl : ITaskService {
  override fun deleteAll(tasks: List<Task>): Unit = throw BadRequestException("Not implemented")

  override fun findAssigneeById(
    projectId: Long,
    taskNumber: Long,
    userId: Long,
  ): List<UserAccount> = emptyList()

  override fun findAssigneeByKey(
    keyId: Long,
    languageId: Long,
    userId: Long,
    type: TaskType?,
  ): List<UserAccount> = emptyList()

  override fun getKeysWithTasks(
    userId: Long,
    keyIds: Collection<Long>,
  ): Map<Long, List<TranslationToTaskView>> = emptyMap()

  override fun getAgencyTasks(agencyId: Long): List<Task> = throw BadRequestException("Not implemented")
}
