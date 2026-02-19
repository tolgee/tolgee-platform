package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class CharacterCaseMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.CHARACTER_CASE_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val base = params.baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    val text = params.text
    if (text.isBlank()) return emptyList()

    val (_, baseFirstChar) = firstLetter(base) ?: return emptyList()
    val (textFirstIndex, textFirstChar) = firstLetter(text) ?: return emptyList()

    val locale = Locale.forLanguageTag(params.languageTag) ?: Locale.ROOT

    if (baseFirstChar.isUpperCase() && textFirstChar.isLowerCase()) {
      val upper = textFirstChar.toString().uppercase(locale)
      return listOf(
        QaCheckResult(
          type = QaCheckType.CHARACTER_CASE_MISMATCH,
          message = QaIssueMessage.QA_CASE_CAPITALIZE,
          replacement = upper,
          positionStart = textFirstIndex,
          positionEnd = textFirstIndex + 1,
        ),
      )
    }

    if (baseFirstChar.isLowerCase() && textFirstChar.isUpperCase()) {
      val lower = textFirstChar.toString().lowercase(locale)
      return listOf(
        QaCheckResult(
          type = QaCheckType.CHARACTER_CASE_MISMATCH,
          message = QaIssueMessage.QA_CASE_LOWERCASE,
          replacement = lower,
          positionStart = textFirstIndex,
          positionEnd = textFirstIndex + 1,
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
