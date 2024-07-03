package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.task.TaskTranslation

class TaskKeyBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<TaskTranslation, TaskKeyBuilder> {
  override var self: TaskTranslation =
    TaskTranslation()
}
