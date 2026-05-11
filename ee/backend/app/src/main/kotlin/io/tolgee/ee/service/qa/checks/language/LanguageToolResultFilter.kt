package io.tolgee.ee.service.qa.checks.language

import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaGlossaryTerm
import io.tolgee.ee.service.qa.checks.HtmlTagParser
import io.tolgee.ee.service.qa.checks.extractArgs
import io.tolgee.ee.service.qa.checks.extractUrls

/**
 * Filters out spelling/grammar results that overlap with placeholders, HTML tags, or URLs.
 * These are usually false positives, plus dedicated checks already cover those elements.
 */
fun filterLanguageToolFalsePositives(
  results: List<QaCheckResult>,
  text: String,
): List<QaCheckResult> {
  if (results.isEmpty()) return results

  val blockedRanges = buildBlockedRanges(text)
  if (blockedRanges.isEmpty()) return results

  return results.filter { result ->
    val start = result.positionStart
    val end = result.positionEnd
    start == null || end == null ||
      !blockedRanges.any { (rangeStart, rangeEnd) ->
        start < rangeEnd && end > rangeStart
      }
  }
}

/**
 * Filters out results that overlap with glossary terms.
 */
fun filterByGlossaryTerms(
  results: List<QaCheckResult>,
  glossaryTerms: List<QaGlossaryTerm>?,
): List<QaCheckResult> {
  if (glossaryTerms.isNullOrEmpty() || results.isEmpty()) return results
  return results.filter { result ->
    val start = result.positionStart
    val end = result.positionEnd
    start == null || end == null ||
      !glossaryTerms.any { term -> start < term.end && end > term.start }
  }
}

private fun buildBlockedRanges(text: String): List<Pair<Int, Int>> {
  val ranges = mutableListOf<Pair<Int, Int>>()

  extractArgs(text)
    ?.filter { it.positionStart != null && it.positionEnd != null }
    ?.forEach { ranges.add(it.positionStart!! to it.positionEnd!!) }

  HtmlTagParser
    .findTags(text)
    .forEach { ranges.add(it.start to it.end) }

  extractUrls(text)
    .forEach { ranges.add(it.start to it.end) }

  return ranges
}
