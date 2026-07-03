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
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText, _ ->
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

    var baseNumbers = extractNumbers(base)
    var textNumbers = extractNumbers(text)

    //compare both numbers if they have comma then make them same and then compare
    val baseNumbersNormalized = baseNumbers.map { it.replace(",", "") }
    val textNumbersNormalized = textNumbers.map { it.replace(",", "") }

    if(baseNumbersNormalized == textNumbersNormalized) {
      baseNumbers = textNumbers
    }

    val baseMultiset = baseNumbers.groupingBy { it }.eachCount()
    val textMultiset = textNumbers.groupingBy { it }.eachCount()
    val missing = mutableListOf<String>()
    for ((number, baseCount) in baseMultiset) {
      val textCount = textMultiset[number] ?: 0
      repeat(maxOf(0, baseCount - textCount)) { missing.add(number) }
    }

    return missing.map { number ->
      QaCheckResult(
        type = QaCheckType.MISSING_NUMBERS,
        message = QaIssueMessage.QA_NUMBERS_MISSING,
        replacement = null,
        positionStart = null,
        positionEnd = null,
        params = mapOf("number" to number),
      )
    }
  }

  companion object {
    private val LENIENT_BLOCK_REGEX = Regex("""\d+(?:[^0-9]\d+)*""")
    private val NUMBER_REGEX = Regex("""\d+([.,]\d+)*""")

    fun extractNumbers(text: String): List<String> {
      return LENIENT_BLOCK_REGEX.findAll(text).mapNotNull { matchResult ->
        val rawBlock = matchResult.value
        
        // Step 2: Cycle through and keep ONLY digits, periods, and commas
        val filteredString = rawBlock.filter { it.isDigit() || it == '.' || it == ',' }
        
        // Step 3: Pass the filtered string through your target regex
        // If it matches completely, we return the value; otherwise, we discard it (mapNotNull)
        if (NUMBER_REGEX.matches(filteredString)) {
            filteredString
        } else {
            null 
        }
      }.toList()
    }
  }
}
