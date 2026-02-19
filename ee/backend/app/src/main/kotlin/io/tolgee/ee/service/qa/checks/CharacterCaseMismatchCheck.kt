package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class CharacterCaseMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.CHARACTER_CASE_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val base = params.baseTranslationText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    val text = params.text
    if (text.isBlank()) return emptyList()

    val baseFirst = firstLetter(base) ?: return emptyList()
    val textFirst = firstLetter(text) ?: return emptyList()

    if (baseFirst.second.isUpperCase() && textFirst.second.isLowerCase()) {
      val index = textFirst.first
      val upper = textFirst.second.uppercaseChar()
      return listOf(
        QaCheckResult(
          type = QaCheckType.CHARACTER_CASE_MISMATCH,
          message = QaIssueMessage.QA_CASE_CAPITALIZE,
          replacement = upper.toString(),
          positionStart = index,
          positionEnd = index + 1,
        ),
      )
    }

    if (baseFirst.second.isLowerCase() && textFirst.second.isUpperCase()) {
      val index = textFirst.first
      val lower = textFirst.second.lowercaseChar()
      return listOf(
        QaCheckResult(
          type = QaCheckType.CHARACTER_CASE_MISMATCH,
          message = QaIssueMessage.QA_CASE_LOWERCASE,
          replacement = lower.toString(),
          positionStart = index,
          positionEnd = index + 1,
        ),
      )
    }

    return emptyList()
  }

  companion object {
    fun firstLetter(text: String): Pair<Int, Char>? {
      for (i in text.indices) {
        val ch = text[i]
        if (ch.isLetter()) return i to ch
      }
      return null
    }
  }
}
