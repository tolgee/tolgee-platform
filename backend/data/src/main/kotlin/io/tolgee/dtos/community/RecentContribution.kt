package io.tolgee.dtos.community

data class RecentContribution(
  /** ISO date. */
  val day: String,
  val projectId: Long,
  val projectName: String,
  val languageTag: String,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)
