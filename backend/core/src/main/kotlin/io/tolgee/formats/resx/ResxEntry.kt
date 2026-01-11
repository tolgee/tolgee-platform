package io.tolgee.formats.resx

data class ResxEntry(
  val key: String,
  val data: String? = null,
  val comment: String? = null,
)
