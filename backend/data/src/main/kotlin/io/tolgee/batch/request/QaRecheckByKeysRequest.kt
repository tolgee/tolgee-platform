package io.tolgee.batch.request

data class QaRecheckByKeysRequest(
  val keyIds: List<Long>,
  val languageIds: List<Long>? = null,
)
