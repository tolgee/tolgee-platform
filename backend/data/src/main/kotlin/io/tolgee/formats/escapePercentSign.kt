package io.tolgee.formats

fun escapePercentSign(string: String): String {
  return string.replace("%", "%%")
}
