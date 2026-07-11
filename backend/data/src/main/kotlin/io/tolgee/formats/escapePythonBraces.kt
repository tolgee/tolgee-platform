package io.tolgee.formats

fun escapePythonBraces(string: String): String {
  return string.replace("{", "{{").replace("}", "}}")
}
