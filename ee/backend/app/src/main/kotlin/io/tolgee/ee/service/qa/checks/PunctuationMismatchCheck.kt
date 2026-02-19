package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class PunctuationMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.PUNCTUATION_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val base = params.baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    val text = params.text
    if (text.isBlank()) return emptyList()

    val baseTrimmed = base.trimEnd()
    val textTrimmed = text.trimEnd()
    if (baseTrimmed.isEmpty() || textTrimmed.isEmpty()) return emptyList()

    val basePunct = trailingPunctuation(baseTrimmed)
    val textPunct = trailingPunctuation(textTrimmed)

    if (basePunct == textPunct) return emptyList()

    val textEnd = textTrimmed.length
    return when {
      basePunct != null && textPunct == null -> {
        listOf(
          QaCheckResult(
            type = QaCheckType.PUNCTUATION_MISMATCH,
            message = QaIssueMessage.QA_PUNCTUATION_ADD,
            replacement = basePunct.toString(),
            positionStart = textEnd,
            positionEnd = textEnd,
            params = mapOf("punctuation" to basePunct.toString()),
          ),
        )
      }

      basePunct == null && textPunct != null -> {
        listOf(
          QaCheckResult(
            type = QaCheckType.PUNCTUATION_MISMATCH,
            message = QaIssueMessage.QA_PUNCTUATION_REMOVE,
            replacement = "",
            positionStart = textEnd - 1,
            positionEnd = textEnd,
            params = mapOf("punctuation" to textPunct.toString()),
          ),
        )
      }

      basePunct != null && textPunct != null -> {
        listOf(
          QaCheckResult(
            type = QaCheckType.PUNCTUATION_MISMATCH,
            message = QaIssueMessage.QA_PUNCTUATION_REPLACE,
            replacement = basePunct.toString(),
            positionStart = textEnd - 1,
            positionEnd = textEnd,
            params = mapOf("punctuation" to textPunct.toString(), "expected" to basePunct.toString()),
          ),
        )
      }

      else -> {
        emptyList()
      }
    }
  }

  companion object {
    private val PUNCTUATION_CHARS = setOf('.', ',', '!', '?', ':', ';')

    fun trailingPunctuation(text: String): Char? {
      val last = text.lastOrNull() ?: return null
      return if (last in PUNCTUATION_CHARS) last else null
    }
  }
}
