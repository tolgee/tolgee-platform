package io.tolgee.events

data class OnQaIssuesIgnored(
  val projectId: Long,
  val translationIds: Collection<Long>,
)
