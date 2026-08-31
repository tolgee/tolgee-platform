package io.tolgee.util

val String.nullIfEmpty: String?
  get() = this.ifEmpty { null }

val String?.nullIfBlank: String?
  get() = this?.takeIf { it.isNotBlank() }

fun String.findAll(regex: Regex): Sequence<String> = regex.findAll(this).map { it.value }

fun String.find(regex: Regex): String? = regex.find(this)?.value
