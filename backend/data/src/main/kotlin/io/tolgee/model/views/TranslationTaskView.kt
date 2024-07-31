package io.tolgee.model.views

import io.tolgee.model.enums.TaskType

class TranslationTaskView(
  val id: Long,
  val done: Boolean,
  val userAssigned: Boolean,
  val type: TaskType,
)
