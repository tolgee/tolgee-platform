package io.tolgee.formats

data class MessageConvertorResult(
  val message: String?,
  val pluralArgName: String?,
  val customValues: Map<String, Any>? = null,
)
