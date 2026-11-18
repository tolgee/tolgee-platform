package io.tolgee.dtos.community

data class Bucket(
  /** ISO date. */
  val start: String,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)
