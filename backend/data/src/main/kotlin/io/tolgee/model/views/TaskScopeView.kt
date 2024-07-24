package io.tolgee.model.views

interface TaskScopeView {
  val taskId: Long?
  val totalItems: Long
  val doneItems: Long
  val baseCharacterCount: Long
  val baseWordCount: Long
}
