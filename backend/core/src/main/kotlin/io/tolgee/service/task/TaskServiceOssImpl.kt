package io.tolgee.service.task

import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.model.views.TranslationToTaskView
import org.springframework.stereotype.Component

@Component
class TaskServiceOssImpl : ITaskService {
  override fun deleteAll(tasks: List<Task>) {
    throw NotImplementedInOss()
  }

  override fun findAssigneeById(
    projectId: Long,
    taskNumber: Long,
    userId: Long,
  ): List<UserAccount> {
    return emptyList()
  }

  override fun findAssigneeByKey(
    keyId: Long,
    languageId: Long,
    userId: Long,
    type: TaskType?,
  ): List<UserAccount> {
    return emptyList()
  }

  override fun getKeysWithTasks(
    userId: Long,
    keyIds: Collection<Long>,
  ): Map<Long, List<TranslationToTaskView>> {
    return emptyMap()
  }

  override fun getAgencyTasks(agencyId: Long): List<Task> {
    throw NotImplementedInOss()
  }
}
