package io.tolgee.model.views

interface TranslationToTaskView {
  var translationId: Long
  var taskId: Long
  var taskDone: Boolean
  var taskAssigned: Boolean
}
