package io.tolgee.model.views

import io.tolgee.model.enums.TaskType

data class TaskSimpleView(
  val id: Long = 0L,
  val type: TaskType = TaskType.TRANSLATE,
)
