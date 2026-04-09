package io.tolgee.ee.service.qa.checks.lines

/**
 * Detects the separator type (CRLF, LF, or CR) from the given lines.
 *
 * Returns UNKNOWN if the lines do not all have the same separator type.
 */
fun detectSeparator(lines: List<Line>): SeparatorType {
  val separators = lines.map { it.type }.filter { it != SeparatorType.UNKNOWN }
  val separator = separators.firstOrNull() ?: return SeparatorType.UNKNOWN
  if (separators.any { it != separator }) {
    return SeparatorType.UNKNOWN
  }
  return separator
}
