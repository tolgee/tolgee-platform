package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class BracketsBalanceCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.BRACKETS_UNBALANCED

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, _ ->
      checkVariant(text)
    }
  }

  private fun checkVariant(text: String): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()

    val stack = ArrayDeque<BracketInfo>()
    val results = mutableListOf<QaCheckResult>()

    for (i in text.indices) {
      val ch = text[i]
      when {
        ch in OPENING_BRACKETS -> {
          stack.addLast(BracketInfo(ch, i))
        }
        ch in CLOSING_BRACKETS -> {
          val expectedOpening = CLOSING_TO_OPENING[ch]
          if (stack.isNotEmpty() && stack.last().char == expectedOpening) {
            stack.removeLast()
          } else {
            // Unmatched closing bracket — suggest removal
            results.add(
              QaCheckResult(
                type = QaCheckType.BRACKETS_UNBALANCED,
                message = QaIssueMessage.QA_BRACKETS_UNMATCHED_CLOSE,
                replacement = "",
                positionStart = i,
                positionEnd = i + 1,
                params = mapOf("bracket" to ch.toString()),
              ),
            )
          }
        }
      }
    }

    // Any remaining opening brackets on the stack are unclosed
    for (info in stack) {
      val closingBracket = OPENING_TO_CLOSING[info.char]!!
      results.add(
        QaCheckResult(
          type = QaCheckType.BRACKETS_UNBALANCED,
          message = QaIssueMessage.QA_BRACKETS_UNCLOSED,
          replacement = closingBracket.toString(),
          positionStart = text.length,
          positionEnd = text.length,
          params = mapOf("bracket" to info.char.toString()),
        ),
      )
    }

    return results
  }

  private data class BracketInfo(
    val char: Char,
    val position: Int,
  )

  companion object {
    private val OPENING_BRACKETS = setOf('(', '[', '{')
    private val CLOSING_BRACKETS = setOf(')', ']', '}')

    private val CLOSING_TO_OPENING =
      mapOf(
        ')' to '(',
        ']' to '[',
        '}' to '{',
      )

    private val OPENING_TO_CLOSING =
      mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
      )
  }
}
