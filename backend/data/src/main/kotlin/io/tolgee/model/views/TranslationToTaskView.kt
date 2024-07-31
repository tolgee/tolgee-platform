package io.tolgee.model.views

import io.tolgee.model.enums.TaskType

interface TranslationToTaskView {
  var translationId: Long
  var taskId: Long
  var taskDone: Boolean
  var taskAssigned: Boolean
  var taskType: TaskType
}
