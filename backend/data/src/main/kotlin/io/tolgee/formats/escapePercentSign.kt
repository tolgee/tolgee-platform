package io.tolgee.formats

/**
 * Escapes percent signs in a string, but preserves common Apple format specifiers.
 * This prevents issues with placeholders like %d and %i being incorrectly escaped as %%d and %%i.
 *
 * @param string The string to escape percent signs in
 * @param preserveFormatSpecifiers If true, preserves common format specifiers like %d, %i, %@, etc.
 * @return The string with percent signs escaped, except for format specifiers if preserveFormatSpecifiers is true
 */
fun escapePercentSign(
  string: String,
  preserveFormatSpecifiers: Boolean = false,
): String {
  if (!preserveFormatSpecifiers) {
    return string.replace("%", "%%")
  }

  // Handle double percent signs first (%%d should become %%%%d)
  val doublePercentProcessed = string.replace("%%", "%%%%")

  // This pattern matches common Apple format specifiers
  // It captures the entire format specifier to be preserved during escaping
  val pattern = Regex("%([@dDiIuUxXoOfFeEgGcCsSpPaAn]|[0-9]+\\$[@dDiIuUxXoOfFeEgGcCsSpPaAn]|l?l?[diouxXfFeEgGaA])")

  // Split the string by format specifiers, escaping each segment properly
  val result = StringBuilder()
  var lastEndIndex = 0

  pattern.findAll(doublePercentProcessed).forEach { matchResult ->
    // Handle text before the current match (escape all % signs that aren't already doubled)
    val textBeforeMatch = doublePercentProcessed.substring(lastEndIndex, matchResult.range.first)
    result.append(textBeforeMatch.replace("%", "%%"))

    // Append the actual format specifier (unescaped)
    result.append(matchResult.value)
    lastEndIndex = matchResult.range.last + 1
  }

  // Handle the remaining text after the last match
  if (lastEndIndex < doublePercentProcessed.length) {
    result.append(doublePercentProcessed.substring(lastEndIndex).replace("%", "%%"))
  }

  return result.toString()
}
