package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class MissingNumbersCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.MISSING_NUMBERS

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

    val baseNumbers = extractNumbers(base)
    val textNumbers = extractNumbers(text)

    val missing = baseNumbers - textNumbers.toSet()

    return missing.map { number ->
      QaCheckResult(
        type = QaCheckType.MISSING_NUMBERS,
        message = QaIssueMessage.QA_NUMBERS_MISSING,
        replacement = null,
        positionStart = 0,
        positionEnd = 0,
        params = mapOf("number" to number),
      )
    }
  }

  companion object {
    private val NUMBER_REGEX = Regex("""\d+([.,]\d+)*""")

    fun extractNumbers(text: String): List<String> {
      return NUMBER_REGEX.findAll(text).map { it.value }.toList()
    }
  }
}
