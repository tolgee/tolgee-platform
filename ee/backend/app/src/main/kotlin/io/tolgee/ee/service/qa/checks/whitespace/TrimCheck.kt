package io.tolgee.ee.service.qa.checks.whitespace

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class TrimCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.TRIM_CHECK

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    // Only run the check for base translation.
    // Spaces mismatch check and Unmatched newlines check take care of propagating
    // whatever preference the user sets up in base translation to all translations.
    if (params.baseLanguageTag != null) return emptyList()

    return QaPluralCheckHelper.runPerVariant(params) { text, _ ->
      checkVariant(text)
    }
  }

  private fun checkVariant(text: String): List<QaCheckResult> {
    if (text.isBlank()) return emptyList()
    val results = mutableListOf<QaCheckResult>()

    checkLeading(text, results)
    checkTrailing(text, results)

    return results
  }

  private fun checkLeading(
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val (leadingNl, _) = extractLeadingNewlines(text)
    if (leadingNl.isNotEmpty()) {
      results.add(
        QaCheckResult(
          type = QaCheckType.TRIM_CHECK,
          message = QaIssueMessage.QA_LEADING_NEWLINES,
          replacement = "",
          positionStart = 0,
          positionEnd = leadingNl.length,
        ),
      )
    }

    val afterNewlines = text.substring(leadingNl.length)
    val (leadingWs, _) = extractLeadingWhitespace(afterNewlines)
    if (leadingWs.isNotEmpty()) {
      results.add(
        QaCheckResult(
          type = QaCheckType.TRIM_CHECK,
          message = QaIssueMessage.QA_LEADING_SPACES,
          replacement = "",
          positionStart = leadingNl.length,
          positionEnd = leadingNl.length + leadingWs.length,
        ),
      )
    }
  }

  private fun checkTrailing(
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val (trailingWs, wsOffset) = extractTrailingWhitespace(text)
    if (trailingWs.isNotEmpty()) {
      results.add(
        QaCheckResult(
          type = QaCheckType.TRIM_CHECK,
          message = QaIssueMessage.QA_TRAILING_SPACES,
          replacement = "",
          positionStart = wsOffset,
          positionEnd = text.length,
        ),
      )
    }

    val textWithoutTrailingSpaces = text.substring(0, wsOffset)
    val (trailingNl, nlOffset) = extractTrailingNewlines(textWithoutTrailingSpaces)
    if (trailingNl.isNotEmpty()) {
      results.add(
        QaCheckResult(
          type = QaCheckType.TRIM_CHECK,
          message = QaIssueMessage.QA_TRAILING_NEWLINES,
          replacement = "",
          positionStart = nlOffset,
          positionEnd = textWithoutTrailingSpaces.length,
        ),
      )
    }
  }
}
