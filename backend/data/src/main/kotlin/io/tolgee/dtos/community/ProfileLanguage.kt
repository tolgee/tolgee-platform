package io.tolgee.dtos.community

data class ProfileLanguage(
  val tag: String,
  val name: String,
  val contributions: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)
