package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.ee.service.qa.checks.whitespace.WHITESPACE_CHARS
import io.tolgee.ee.service.qa.checks.whitespace.extractLeadingWhitespace
import io.tolgee.ee.service.qa.checks.whitespace.extractTrailingWhitespace
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class SpacesMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.SPACES_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText, _ ->
      checkVariant(text, baseText)
    }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
  ): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()

    val results = mutableListOf<QaCheckResult>()

    results += filterResultsInBlockedRanges(checkDoubledSpaces(text), text)

    // Source-comparison checks — only when a base text is available.
    if (!baseText.isNullOrBlank()) {
      checkSegmentMatches(
        baseText,
        text,
        results,
        QaIssueMessage.QA_SPACES_LEADING_ADDED,
        QaIssueMessage.QA_SPACES_LEADING_REMOVED,
        ::extractLeadingWhitespace,
      )
      checkSegmentMatches(
        baseText,
        text,
        results,
        QaIssueMessage.QA_SPACES_TRAILING_ADDED,
        QaIssueMessage.QA_SPACES_TRAILING_REMOVED,
        ::extractTrailingWhitespace,
      )
    }

    return results
  }

  private fun checkSegmentMatches(
    base: String,
    text: String,
    results: MutableList<QaCheckResult>,
    messageWhenAdded: QaIssueMessage,
    messageWhenRemoved: QaIssueMessage,
    extractSegment: (String) -> Pair<String, Int>,
  ) {
    val (baseSegment, _) = extractSegment(base)
    val (textSegment, textOffset) = extractSegment(text)

    if (baseSegment == textSegment) return

    val commonPrefixLen = textSegment.commonPrefixWith(baseSegment).length
    val textRemainder = textSegment.substring(commonPrefixLen)
    val baseRemainder = baseSegment.substring(commonPrefixLen)
    val commonSuffixLen = textRemainder.commonSuffixWith(baseRemainder).length

    val editStart = textOffset + commonPrefixLen
    val editEnd = textOffset + textSegment.length - commonSuffixLen
    val replacement = baseSegment.substring(commonPrefixLen, baseSegment.length - commonSuffixLen)

    val message =
      if (replacement.length <= (editEnd - editStart)) {
        messageWhenAdded
      } else {
        messageWhenRemoved
      }

    results.add(
      QaCheckResult(
        type = QaCheckType.SPACES_MISMATCH,
        message = message,
        replacement = replacement,
        positionStart = editStart,
        positionEnd = editEnd,
      ),
    )
  }

  private fun checkDoubledSpaces(text: String): List<QaCheckResult> {
    val interiorStart = text.length - text.trimStart(*WHITESPACE_CHARS).length
    val interiorEnd = text.trimEnd(*WHITESPACE_CHARS).length
    if (interiorStart >= interiorEnd) return emptyList()

    val results = mutableListOf<QaCheckResult>()
    val interior = text.substring(interiorStart, interiorEnd)
    for (match in DOUBLED_SPACES_REGEX.findAll(interior)) {
      val runStart = interiorStart + match.range.first
      val runEnd = interiorStart + match.range.last + 1
      // Keep the first space; flag only the extras as removable.
      val firstExtraSpace = runStart + 1
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_DOUBLED,
          replacement = "",
          positionStart = firstExtraSpace,
          positionEnd = runEnd,
        ),
      )
    }
    return results
  }

  companion object {
    private val DOUBLED_SPACES_REGEX = Regex("[ \u00A0]{2,}")
  }
}
