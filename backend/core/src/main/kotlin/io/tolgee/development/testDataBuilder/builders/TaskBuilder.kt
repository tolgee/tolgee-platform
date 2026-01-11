package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.task.Task

class TaskBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Task, TaskBuilder> {
  override var self: Task =
    Task().apply {
      this.project = projectBuilder.self
    }
}
