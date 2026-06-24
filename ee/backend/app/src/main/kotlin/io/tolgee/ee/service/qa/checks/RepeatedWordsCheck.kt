package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class RepeatedWordsCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.REPEATED_WORDS

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, _, _ ->
      checkVariant(text)
    }
  }

  private fun checkVariant(text: String): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()

    val words = WORD_REGEX.findAll(text).toList()
    if (words.size < 2) return emptyList()

    val results = mutableListOf<QaCheckResult>()

    for (i in 1 until words.size) {
      val prev = words[i - 1]
      val curr = words[i]
      if (!prev.value.equals(curr.value, ignoreCase = true)) continue
      if (!isPlainSpaceSeparator(text, prev.range.last + 1, curr.range.first)) continue
      results.add(
        QaCheckResult(
          type = QaCheckType.REPEATED_WORDS,
          message = QaIssueMessage.QA_REPEATED_WORD,
          replacement = "",
          positionStart = prev.range.last + 1,
          positionEnd = curr.range.last + 1,
          params = mapOf("word" to curr.value),
        ),
      )
    }

    return filterResultsInBlockedRanges(results, text)
  }

  /**
   * Returns true when text in given range is non-empty and consists only of
   * regular spaces (U+0020) and/or non-breaking spaces (U+00A0).
   */
  private fun isPlainSpaceSeparator(
    text: String,
    start: Int,
    end: Int,
  ): Boolean {
    if (start >= end) return false
    for (i in start until end) {
      val c = text[i]
      if (c != SPACE && c != NBSP) return false
    }
    return true
  }

  companion object {
    private val WORD_REGEX = Regex("""\p{L}[\p{L}\p{N}]*""")
    private const val SPACE = '\u0020'
    private const val NBSP = '\u00A0'
  }
}
