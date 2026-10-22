package io.tolgee.dtos.community

data class LanguageCounts(
  val languageId: Long,
  val suggested: Long,
  val accepted: Long,
  val declined: Long,
)
