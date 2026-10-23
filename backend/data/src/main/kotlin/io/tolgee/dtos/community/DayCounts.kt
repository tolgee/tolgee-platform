package io.tolgee.dtos.community

data class DayCounts(
  /** ISO date. */
  val day: String,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)
