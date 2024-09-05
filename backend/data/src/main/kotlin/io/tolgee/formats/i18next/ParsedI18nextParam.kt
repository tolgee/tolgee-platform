package io.tolgee.formats.i18next

data class ParsedI18nextParam(
  val key: String? = null,
  val nestedKey: String? = null,
  val format: String? = null,
  val fullMatch: String,
)
