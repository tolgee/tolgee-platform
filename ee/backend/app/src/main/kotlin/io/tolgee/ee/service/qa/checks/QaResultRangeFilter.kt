package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckResult

/**
 * Filters out results that overlap with placeholders, HTML tags, or URLs.
 * These are usually false positives, plus dedicated checks already cover those elements.
 *
 * Used by text-scanning checks (Spelling, Grammar, RepeatedWords) that operate on raw
 * translation text and would otherwise misclassify tag names, ICU placeholder identifiers,
 * or URL fragments as content.
 */
fun filterResultsInBlockedRanges(
  results: List<QaCheckResult>,
  text: String,
): List<QaCheckResult> {
  if (results.isEmpty()) return results

  val blockedRanges = buildBlockedRanges(text)
  if (blockedRanges.isEmpty()) return results

  return results.filter { result ->
    blockedRanges.none { result.overlaps(it.start, it.end) }
  }
}

/**
 * Half-open `[start, end)` range covering text that should not produce QA findings.
 */
internal data class BlockedRange(
  val start: Int,
  val end: Int,
)

/**
 * Returns true when this result's `[positionStart, positionEnd)` overlaps the given
 * half-open range. Results with null positions never overlap.
 */
internal fun QaCheckResult.overlaps(
  rangeStart: Int,
  rangeEnd: Int,
): Boolean {
  val start = positionStart ?: return false
  val end = positionEnd ?: return false
  return start < rangeEnd && end > rangeStart
}

private fun buildBlockedRanges(text: String): List<BlockedRange> {
  val ranges = mutableListOf<BlockedRange>()

  extractArgs(text)?.forEach { arg ->
    val start = arg.positionStart ?: return@forEach
    val end = arg.positionEnd ?: return@forEach
    ranges.add(BlockedRange(start, end))
  }

  HtmlTagParser
    .findTags(text)
    .forEach { ranges.add(BlockedRange(it.start, it.end)) }

  extractUrls(text)
    .forEach { ranges.add(BlockedRange(it.start, it.end)) }

  return ranges
}
