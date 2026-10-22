package io.tolgee.dtos.community

data class WeekBucket(
  /** ISO date of the week's Monday. */
  val weekStart: String,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)
