package io.tolgee.util

fun Regex.replaceEntire(
  input: CharSequence,
  replacement: String,
): String? {
  if (!matches(input)) {
    return null
  }

  return replaceFirst(input, replacement)
}
