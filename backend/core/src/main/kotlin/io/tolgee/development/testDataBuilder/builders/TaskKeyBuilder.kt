package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.task.TaskKey

class TaskKeyBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<TaskKey, TaskKeyBuilder> {
  override var self: TaskKey =
    TaskKey()
}
