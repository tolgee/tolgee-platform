package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class DifferentUrlsCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.DIFFERENT_URLS

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText ->
      checkVariant(text, baseText)
    }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    if (text.isBlank()) return emptyList()

    val baseUrls = extractUrls(base)
    val textUrlMatches = extractUrls(text)

    val baseUrlStrings = baseUrls.map { it.url }
    val textUrlStrings = textUrlMatches.map { it.url }

    val baseMultiset = baseUrlStrings.groupingBy { it }.eachCount()
    val textMultiset = textUrlStrings.groupingBy { it }.eachCount()

    val allUrls = baseMultiset.keys + textMultiset.keys
    val missingUrls = mutableListOf<String>()
    val extraUrlMatches = mutableListOf<UrlMatch>()

    for (url in allUrls) {
      val baseCount = baseMultiset[url] ?: 0
      val textCount = textMultiset[url] ?: 0
      if (baseCount > textCount) {
        repeat(baseCount - textCount) { missingUrls.add(url) }
      } else if (textCount > baseCount) {
        val matches = textUrlMatches.filter { it.url == url }
        matches.takeLast(textCount - baseCount).forEach { extraUrlMatches.add(it) }
      }
    }

    val results = mutableListOf<QaCheckResult>()

    // Pair missing + extra as "replace" where possible
    val replaceCount = minOf(missingUrls.size, extraUrlMatches.size)
    for (i in 0 until replaceCount) {
      val expectedUrl = missingUrls[i]
      val wrongMatch = extraUrlMatches[i]
      results.add(
        QaCheckResult(
          type = QaCheckType.DIFFERENT_URLS,
          message = QaIssueMessage.QA_URL_REPLACE,
          replacement = expectedUrl,
          positionStart = wrongMatch.start,
          positionEnd = wrongMatch.end,
          params = mapOf("url" to wrongMatch.url, "expected" to expectedUrl),
        ),
      )
    }

    // Remaining missing URLs (not paired)
    for (i in replaceCount until missingUrls.size) {
      results.add(
        QaCheckResult(
          type = QaCheckType.DIFFERENT_URLS,
          message = QaIssueMessage.QA_URL_MISSING,
          replacement = null,
          positionStart = null,
          positionEnd = null,
          params = mapOf("url" to missingUrls[i]),
        ),
      )
    }

    // Remaining extra URLs (not paired)
    for (i in replaceCount until extraUrlMatches.size) {
      val m = extraUrlMatches[i]
      results.add(
        QaCheckResult(
          type = QaCheckType.DIFFERENT_URLS,
          message = QaIssueMessage.QA_URL_EXTRA,
          replacement = "",
          positionStart = m.start,
          positionEnd = m.end,
          params = mapOf("url" to m.url),
        ),
      )
    }

    return results
  }

  companion object {
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
  }
}
