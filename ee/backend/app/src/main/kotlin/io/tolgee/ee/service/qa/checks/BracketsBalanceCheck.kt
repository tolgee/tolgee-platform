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
    val bracketSets = getBracketSets(params.icuPlaceholders)
    return QaPluralCheckHelper.runPerVariant(params) { text, _ ->
      checkVariant(text, bracketSets)
    }
  }

  private fun checkVariant(
    text: String,
    bracketSets: BracketSets,
  ): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()

    val stack = ArrayDeque<BracketInfo>()
    val results = mutableListOf<QaCheckResult>()

    for (i in text.indices) {
      val ch = text[i]
      when {
        ch in bracketSets.opening -> {
          stack.addLast(BracketInfo(ch, i))
        }
        ch in bracketSets.closing -> {
          val expectedOpening = bracketSets.closingToOpening[ch]
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
      val closingBracket = bracketSets.openingToClosing[info.char]!!
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

  data class BracketSets(
    val opening: Set<Char>,
    val closing: Set<Char>,
    val closingToOpening: Map<Char, Char>,
    val openingToClosing: Map<Char, Char>,
  )

  companion object {
    private val ALL_BRACKET_SETS =
      BracketSets(
        opening = setOf('(', '[', '{'),
        closing = setOf(')', ']', '}'),
        closingToOpening = mapOf(')' to '(', ']' to '[', '}' to '{'),
        openingToClosing = mapOf('(' to ')', '[' to ']', '{' to '}'),
      )

    private val NON_ICU_BRACKET_SETS =
      BracketSets(
        opening = setOf('(', '['),
        closing = setOf(')', ']'),
        closingToOpening = mapOf(')' to '(', ']' to '['),
        openingToClosing = mapOf('(' to ')', '[' to ']'),
      )

    fun getBracketSets(icuPlaceholders: Boolean): BracketSets =
      if (icuPlaceholders) NON_ICU_BRACKET_SETS else ALL_BRACKET_SETS
  }
}
