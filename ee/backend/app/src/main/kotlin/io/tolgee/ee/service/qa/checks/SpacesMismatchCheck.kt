package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class SpacesMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.SPACES_MISMATCH

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

    val results = mutableListOf<QaCheckResult>()

    checkSegmentMatches(
      base,
      text,
      results,
      QaIssueMessage.QA_SPACES_LEADING_ADDED,
      QaIssueMessage.QA_SPACES_LEADING_REMOVED,
      ::extractLeadingWhitespace,
    )
    checkSegmentMatches(
      base,
      text,
      results,
      QaIssueMessage.QA_SPACES_TRAILING_ADDED,
      QaIssueMessage.QA_SPACES_TRAILING_REMOVED,
      ::extractTrailingWhitespace,
    )
    checkDoubledSpaces(text, results)

    return results
  }

  private fun checkSegmentMatches(
    base: String,
    text: String,
    results: MutableList<QaCheckResult>,
    addingTextMessage: QaIssueMessage,
    removingTextMessage: QaIssueMessage,
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
        addingTextMessage
      } else {
        removingTextMessage
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

  private fun checkDoubledSpaces(
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val interiorStart = text.length - text.trimStart(*WHITESPACE_CHARS).length
    val interiorEnd = text.trimEnd(*WHITESPACE_CHARS).length
    if (interiorStart >= interiorEnd) return

    val interior = text.substring(interiorStart, interiorEnd)
    val regex = Regex("[ \u00A0]{2,}")
    for (match in regex.findAll(interior)) {
      val absStart = interiorStart + match.range.first
      val absEnd = interiorStart + match.range.last + 1
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_DOUBLED,
          replacement = "",
          positionStart = absStart + 1,
          positionEnd = absEnd,
        ),
      )
    }
  }

  companion object {
    private val WHITESPACE_CHARS = charArrayOf(' ', '\t', '\u00A0')

    fun extractLeadingWhitespace(text: String): Pair<String, Int> {
      val ws = text.takeWhile { it in WHITESPACE_CHARS }
      return Pair(ws, 0)
    }

    fun extractTrailingWhitespace(text: String): Pair<String, Int> {
      val ws = text.takeLastWhile { it in WHITESPACE_CHARS }
      return Pair(ws, text.length - ws.length)
    }
  }
}
