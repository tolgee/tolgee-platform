package io.tolgee.formats

/**
 * Unlike the [String.replace] method, this method allows to replace both matched and unmatched parts of the string.
 */
fun String.replaceMatchedAndUnmatched(
  string: String,
  regex: Regex,
  matchedCallback: (MatchResult) -> String,
  unmatchedCallback: (String) -> String,
): String {
  var lastIndex = 0
  val result = StringBuilder()

  for (match in regex.findAll(string)) {
    val unmatchedPart = string.substring(lastIndex until match.range.first)
    result.append(unmatchedCallback(unmatchedPart))
    result.append(matchedCallback(match))
    lastIndex = match.range.last + 1
  }

  val finalUnmatchedPart = string.substring(lastIndex)
  result.append(unmatchedCallback(finalUnmatchedPart))

  return result.toString()
}
