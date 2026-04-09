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
    return QaPluralCheckHelper.runPerVariant(params) { text, _ ->
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
      if (prev.value.equals(curr.value, ignoreCase = true)) {
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
    }

    return results
  }

  companion object {
    private val WORD_REGEX = Regex("""\p{L}[\p{L}\p{N}]*""")
  }
}
