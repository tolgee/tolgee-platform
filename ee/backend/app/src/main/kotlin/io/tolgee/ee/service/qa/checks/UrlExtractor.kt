package io.tolgee.ee.service.qa.checks

private val URL_REGEX =
  Regex(
    """(?:https?|ftp)://[^\s<>"\])}]+|www\.[^\s<>"\])}]+""",
    RegexOption.IGNORE_CASE,
  )

private val TRAILING_STRIP_CHARS = setOf('.', ',', '!', '?', ';', ':')

data class UrlMatch(
  val url: String,
  val start: Int,
  val end: Int,
)

fun extractUrls(text: String): List<UrlMatch> {
  return URL_REGEX
    .findAll(text)
    .mapNotNull { match ->
      var value = match.value
      while (value.isNotEmpty() && value.last() in TRAILING_STRIP_CHARS) {
        value = value.dropLast(1)
      }
      if (value.isEmpty()) {
        null
      } else {
        UrlMatch(url = value, start = match.range.first, end = match.range.first + value.length)
      }
    }.toList()
}
