package io.tolgee.dtos.queryResults.organization

interface IQuickStart {
  val finished: Boolean
  val completedSteps: Array<String>
  val open: Boolean
}
