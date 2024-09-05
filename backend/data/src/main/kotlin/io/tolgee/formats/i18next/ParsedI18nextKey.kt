package io.tolgee.formats.i18next

data class ParsedI18nextKey(
  val key: String? = null,
  val plural: String? = null,
  val fullMatch: String,
)
