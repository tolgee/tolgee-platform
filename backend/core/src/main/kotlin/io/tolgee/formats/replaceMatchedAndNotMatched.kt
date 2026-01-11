package io.tolgee.formats

/**
 * Unlike the [String.replace] method, this method allows to replace both matched and unmatched parts of the string.
 */
fun String.replaceMatchedAndUnmatched(
  regex: Regex,
  matchedCallback: (match: MatchResult) -> String,
  unmatchedCallback: (String) -> String,
): String {
  var lastIndex = 0
  val result = StringBuilder()

  val matches = regex.findAll(this)
  for (match in matches) {
    val unmatchedPart = substring(lastIndex until match.range.first)
    result.append(unmatchedCallback(unmatchedPart))
    result.append(matchedCallback(match))
    lastIndex = match.range.last + 1
  }

  val finalUnmatchedPart = substring(lastIndex)
  result.append(unmatchedCallback(finalUnmatchedPart))

  return result.toString()
}
