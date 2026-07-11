package io.tolgee.repository

import io.tolgee.model.task.TaskKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TaskKeyRepository : JpaRepository<TaskKey, Long> {
  fun findByTaskIdAndKeyId(
    taskId: Long,
    keyId: Long,
  ): TaskKey?

  fun deleteAllByKeyIdIn(keyIds: Collection<Long>)

  @Modifying
  @Query("DELETE FROM TaskKey tk WHERE tk.key.project.id = :projectId")
  fun deleteAllByKeyProjectId(projectId: Long)
}
