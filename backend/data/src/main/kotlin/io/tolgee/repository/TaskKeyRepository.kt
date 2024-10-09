package io.tolgee.repository

import io.tolgee.model.task.TaskKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskKeyRepository : JpaRepository<TaskKey, Long> {
  fun findByTaskIdAndKeyId(
    taskId: Long,
    keyId: Long,
  ): TaskKey?
}
