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
    val bracketChars = getBracketChars(params.icuPlaceholders)
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText ->
      checkVariant(text, baseText, bracketChars)
    }
  }

  private fun checkVariant(
    text: String,
    baseText: String?,
    bracketChars: CharArray,
  ): List<QaCheckResult> {
    val base = baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    if (text.isBlank()) return emptyList()

    val baseCounts = countBrackets(base, bracketChars)
    val textCounts = countBrackets(text, bracketChars)

    val results = mutableListOf<QaCheckResult>()
    val missingBrackets = mutableListOf<Pair<Char, Int>>() // bracket to missing count

    for (bracket in bracketChars) {
      val baseCount = baseCounts[bracket] ?: 0
      val textCount = textCounts[bracket] ?: 0

      if (textCount > baseCount) {
        // Extra brackets in translation — report each extra occurrence (they have unique positions)
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
        continue
      }

      if (textCount < baseCount) {
        missingBrackets.add(bracket to (baseCount - textCount))
        continue
      }
    }

    // Aggregate all missing brackets into a single issue to avoid identity conflicts
    if (missingBrackets.isNotEmpty()) {
      val description = missingBrackets.joinToString(", ") { (b, n) -> if (n > 1) "$b ×$n" else b.toString() }
      val totalMissing = missingBrackets.sumOf { it.second }
      results.add(
        QaCheckResult(
          type = QaCheckType.BRACKETS_MISMATCH,
          message = QaIssueMessage.QA_BRACKETS_MISSING,
          replacement = null,
          positionStart = null,
          positionEnd = null,
          params = mapOf("brackets" to description, "count" to totalMissing.toString()),
        ),
      )
    }

    return results
  }

  companion object {
    private val ALL_BRACKET_CHARS = charArrayOf('(', ')', '[', ']', '{', '}')
    private val NON_ICU_BRACKET_CHARS = charArrayOf('(', ')', '[', ']')

    fun getBracketChars(icuPlaceholders: Boolean): CharArray =
      if (icuPlaceholders) NON_ICU_BRACKET_CHARS else ALL_BRACKET_CHARS

    private fun countBrackets(
      text: String,
      bracketChars: CharArray,
    ): Map<Char, Int> {
      val counts = mutableMapOf<Char, Int>()
      for (ch in text) {
        if (ch in bracketChars) {
          counts[ch] = (counts[ch] ?: 0) + 1
        }
      }
      return counts
    }

    private fun findBracketPositions(
      text: String,
      bracket: Char,
    ): List<Int> {
      return text.indices.filter { text[it] == bracket }
    }
  }
}
