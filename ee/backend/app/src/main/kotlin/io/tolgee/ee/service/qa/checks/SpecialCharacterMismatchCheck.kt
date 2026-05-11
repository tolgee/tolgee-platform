package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class SpecialCharacterMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.SPECIAL_CHARACTER_MISMATCH

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

    val baseChars = extractSpecialChars(base)
    val textChars = extractSpecialChars(text)

    val results = mutableListOf<QaCheckResult>()

    // Aggregate all missing characters into a single issue to avoid QA issue identity conflicts
    val missingChars = subtractMultiset(baseChars, textChars)
    if (missingChars.isNotEmpty()) {
      val missingByChar = missingChars.groupingBy { it }.eachCount()
      val description = missingByChar.entries.joinToString(", ") { (c, n) -> if (n > 1) "$c ×$n" else c.toString() }
      results.add(
        QaCheckResult(
          type = QaCheckType.SPECIAL_CHARACTER_MISMATCH,
          message = QaIssueMessage.QA_SPECIAL_CHAR_MISSING,
          replacement = null,
          positionStart = null,
          positionEnd = null,
          params = mapOf("characters" to description, "count" to missingChars.size.toString()),
        ),
      )
    }

    // Characters in translation but not in base
    val addedChars = subtractMultiset(textChars, baseChars)
    for (char in addedChars.toSet()) {
      val count = addedChars.count { it == char }
      val positions = findAllOccurrences(text, char)
      // Report the last N occurrences as the "extra" ones
      positions.takeLast(count).forEach { index ->
        results.add(
          QaCheckResult(
            type = QaCheckType.SPECIAL_CHARACTER_MISMATCH,
            message = QaIssueMessage.QA_SPECIAL_CHAR_ADDED,
            replacement = "",
            positionStart = index,
            positionEnd = index + 1,
            params = mapOf("character" to char.toString()),
          ),
        )
      }
    }

    return results
  }

  companion object {
    val SPECIAL_CHARS =
      setOf(
        '$',
        '€',
        '£',
        '¥',
        '@',
        '#',
        '&',
        '©',
        '®',
        '™',
        '°',
      )

    fun extractSpecialChars(text: String): List<Char> {
      return text.filter { it in SPECIAL_CHARS }.toList()
    }

    private fun subtractMultiset(
      a: List<Char>,
      b: List<Char>,
    ): List<Char> {
      val remaining = a.toMutableList()
      for (ch in b) {
        remaining.remove(ch)
      }
      return remaining
    }

    private fun findAllOccurrences(
      text: String,
      char: Char,
    ): List<Int> {
      return text.indices.filter { text[it] == char }
    }
  }
}
