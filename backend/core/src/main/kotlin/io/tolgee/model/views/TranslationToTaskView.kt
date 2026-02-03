package io.tolgee.model.views

import io.tolgee.model.enums.TaskType

interface TranslationToTaskView {
  var keyId: Long
  var languageId: Long
  var languageTag: String
  var taskNumber: Long
  var taskDone: Boolean
  var taskAssigned: Boolean
  var taskType: TaskType
}
