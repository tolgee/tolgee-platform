package io.tolgee.fixtures

fun String.removeSlashSuffix(): String {
  return REGEX.replace(this, "")
}

private val REGEX by lazy {
  Regex("/+$")
}
