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
class SpellingCheck(
  private val languageToolService: LanguageToolService,
) : QaCheck {
  override val type: QaCheckType = QaCheckType.SPELLING
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
    return matches
      .filter { isSpellingRule(it) }
      .map { match ->
        QaCheckResult(
          type = QaCheckType.SPELLING,
          message = QaIssueMessage.QA_SPELLING_ERROR,
          replacement = match.suggestedReplacements.firstOrNull(),
          positionStart = match.fromPos,
          positionEnd = match.toPos,
          params = mapOf("word" to text.substring(match.fromPos, match.toPos)),
        )
      }
  }
}
