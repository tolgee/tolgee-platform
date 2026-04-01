package io.tolgee.ee.service.qa.checks.language

import io.tolgee.ee.service.qa.LanguageToolService
import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class GrammarCheck(
  private val languageToolService: LanguageToolService,
) : QaCheck {
  override val type: QaCheckType = QaCheckType.GRAMMAR
  override val debounceDuration: Long = 500L

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, _ ->
      checkVariant(text, params.languageTag)
    }
  }

  private fun checkVariant(
    text: String,
    languageTag: String,
  ): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()

    val matches = languageToolService.check(text, languageTag)
    val results =
      matches
        .filter { !isSpellingRule(it) }
        .filter { !isSentenceRule(it) }
        .map { match ->
          QaCheckResult(
            type = QaCheckType.GRAMMAR,
            message = QaIssueMessage.QA_GRAMMAR_ERROR,
            replacement = match.suggestedReplacements.firstOrNull(),
            positionStart = match.fromPos,
            positionEnd = match.toPos,
            params = mapOf("message" to match.message),
          )
        }
    return filterLanguageToolFalsePositives(results, text)
  }
}
