package io.tolgee.util

val String.nullIfEmpty: String?
  get() = this.ifEmpty { null }
