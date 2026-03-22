package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class BracketsMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.BRACKETS_MISMATCH

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

    val baseCounts = countBrackets(base)
    val textCounts = countBrackets(text)

    val results = mutableListOf<QaCheckResult>()

    for (bracket in BRACKET_CHARS) {
      val baseCount = baseCounts[bracket] ?: 0
      val textCount = textCounts[bracket] ?: 0

      if (baseCount == textCount) continue

      if (textCount > baseCount) {
        // Extra brackets in translation — report each extra occurrence
        val positions = findBracketPositions(text, bracket)
        for (pos in positions.drop(baseCount)) {
          results.add(
            QaCheckResult(
              type = QaCheckType.BRACKETS_MISMATCH,
              message = QaIssueMessage.QA_BRACKETS_EXTRA,
              replacement = null,
              positionStart = pos,
              positionEnd = pos + 1,
              params = mapOf("bracket" to bracket.toString()),
            ),
          )
        }
      } else {
        // Missing brackets in translation — no specific position
        repeat(baseCount - textCount) {
          results.add(
            QaCheckResult(
              type = QaCheckType.BRACKETS_MISMATCH,
              message = QaIssueMessage.QA_BRACKETS_MISSING,
              replacement = null,
              positionStart = null,
              positionEnd = null,
              params = mapOf("bracket" to bracket.toString()),
            ),
          )
        }
      }
    }

    return results
  }

  companion object {
    val BRACKET_CHARS = charArrayOf('(', ')', '[', ']', '{', '}')

    private fun countBrackets(text: String): Map<Char, Int> {
      val counts = mutableMapOf<Char, Int>()
      for (ch in text) {
        if (ch in BRACKET_CHARS) {
          counts[ch] = (counts[ch] ?: 0) + 1
        }
      }
      return counts
    }

    private fun findBracketPositions(
      text: String,
      bracket: Char,
    ): List<Int> {
      val positions = mutableListOf<Int>()
      for (i in text.indices) {
        if (text[i] == bracket) {
          positions.add(i)
        }
      }
      return positions
    }
  }
}
