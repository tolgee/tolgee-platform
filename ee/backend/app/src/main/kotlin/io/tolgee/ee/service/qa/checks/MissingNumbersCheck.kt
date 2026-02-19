package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class MissingNumbersCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.MISSING_NUMBERS

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val base = params.baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    val text = params.text
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
