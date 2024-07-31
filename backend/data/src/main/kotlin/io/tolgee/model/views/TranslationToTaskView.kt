package io.tolgee.model.views

import io.tolgee.model.enums.TaskState

interface TranslationToTaskView {
  var translationId: Long
  var taskId: Long
  var taskState: TaskState
  var taskAssigned: Boolean
}
