package io.tolgee.ee.service.qa.checks

private val URL_REGEX =
  Regex(
    """(?:https?|ftp)://[^\s<>"]+|www\.[^\s<>"]+""",
    RegexOption.IGNORE_CASE,
  )

private val TRAILING_STRIP_CHARS = setOf('.', ',', '!', '?', ';', ':')

private val BRACKET_PAIRS = mapOf(')' to '(', ']' to '[', '}' to '{')

data class UrlMatch(
  val url: String,
  val start: Int,
  val end: Int,
)

fun extractUrls(text: String): List<UrlMatch> {
  return URL_REGEX
    .findAll(text)
    .mapNotNull { match ->
      var url = match.value
      while (url.isNotEmpty() && (url.last() in BRACKET_PAIRS || url.last() in TRAILING_STRIP_CHARS)) {
        if (url.last() in TRAILING_STRIP_CHARS) {
          url = url.dropLast(1)
          continue
        }
        val closer = url.last()
        val opener = BRACKET_PAIRS.getValue(closer)
        if (url.count { it == closer } > url.count { it == opener }) {
          url = url.dropLast(1)
          continue
        }

        // This one is balanced within the scope of the URL
        break
      }

      if (url.isEmpty()) {
        return@mapNotNull null
      }

      UrlMatch(url = url, start = match.range.first, end = match.range.first + url.length)
    }.toList()
}
