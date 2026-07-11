package io.tolgee.ee.service.qa.checks.whitespace

private val NEWLINE_CHARS = charArrayOf('\n', '\r')

val WHITESPACE_CHARS = charArrayOf(' ', '\t', '\u00A0')

fun extractLeadingWhitespace(text: String): Pair<String, Int> {
  val ws = text.takeWhile { it in WHITESPACE_CHARS }
  return ws to 0
}

fun extractTrailingWhitespace(text: String): Pair<String, Int> {
  val ws = text.takeLastWhile { it in WHITESPACE_CHARS }
  return ws to text.length - ws.length
}

fun extractLeadingNewlines(text: String): Pair<String, Int> {
  val nl = text.takeWhile { it in NEWLINE_CHARS }
  return nl to 0
}

fun extractTrailingNewlines(text: String): Pair<String, Int> {
  val nl = text.takeLastWhile { it in NEWLINE_CHARS }
  return nl to text.length - nl.length
}
