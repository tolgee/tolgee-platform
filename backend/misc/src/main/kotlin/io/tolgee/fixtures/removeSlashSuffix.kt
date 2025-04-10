package io.tolgee.fixtures

fun String.removeSlashSuffix(): String = REGEX.replace(this, "")

private val REGEX by lazy {
  Regex("/+$")
}
