package io.tolgee.repository

import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskTranslation
import io.tolgee.model.task.TaskTranslationId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaskTranslationRepository : JpaRepository<TaskTranslation, TaskTranslationId> {
  fun deleteByTask(task: Task)
}
