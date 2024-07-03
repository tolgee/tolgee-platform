package io.tolgee.repository

import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.task.TaskKeyId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskKeyRepository : JpaRepository<TaskKey, TaskKeyId> {
  fun deleteByTask(task: Task)
}
