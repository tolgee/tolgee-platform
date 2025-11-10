package io.tolgee.util

/**
 * Splits the input string based on the matches of the provided regular expression.
 * The resulting list includes the substrings between matches, as well as the matches themselves.
 * If no matches are found, the result will contain a single empty string.
 *
 * @param regex The regular expression used to find matches in the input string.
 * @param input The input string to be split and matched.
 * @return A list of substrings, including the text between matches and the matches themselves.
 */
fun regexSplitAndMatch(
  regex: Regex,
  input: String,
): List<String> {
  val result = mutableListOf<String>()
  var lastIndex = 0

  regex.findAll(input).forEach { match ->
    // Add text before the match if exists
    if (match.range.first > lastIndex) {
      result.add(input.substring(lastIndex, match.range.first))
    }
    // Add the match itself
    result.add(match.value)
    lastIndex = match.range.last + 1
  }

  // Add remaining text after last match
  if (lastIndex < input.length) {
    result.add(input.substring(lastIndex))
  }

  if (result.isEmpty()) {
    result.add("")
  }

  return result
}
