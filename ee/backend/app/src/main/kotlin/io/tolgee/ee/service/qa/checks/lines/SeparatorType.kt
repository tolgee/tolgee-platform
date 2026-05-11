package io.tolgee.ee.service.qa.checks.lines

enum class SeparatorType(
  val separator: String,
) {
  CRLF("\r\n"),
  LF("\n"),
  CR("\r"),
  UNKNOWN("\n"), // Assume LF
}
