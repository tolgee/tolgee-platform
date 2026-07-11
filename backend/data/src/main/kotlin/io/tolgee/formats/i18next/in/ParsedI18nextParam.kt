package io.tolgee.formats.i18next.`in`

data class ParsedI18nextParam(
  val key: String? = null,
  val nestedKey: String? = null,
  val format: String? = null,
  val keepUnescaped: Boolean = false,
  val fullMatch: String,
)
