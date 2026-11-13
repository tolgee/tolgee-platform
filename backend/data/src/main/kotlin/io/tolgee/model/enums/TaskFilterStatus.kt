package io.tolgee.model.enums

enum class TaskFilterStatus {
  IN_OPEN_TASK,
  NOT_IN_OPEN_TASK,
  HAS_BEEN_IN_TASK,
  NEVER_IN_TASK,
  ;

  val openOnly: Boolean get() = this == IN_OPEN_TASK || this == NOT_IN_OPEN_TASK

  val negated: Boolean get() = this == NOT_IN_OPEN_TASK || this == NEVER_IN_TASK
}
