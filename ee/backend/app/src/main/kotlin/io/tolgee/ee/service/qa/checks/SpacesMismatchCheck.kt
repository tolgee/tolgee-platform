package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class SpacesMismatchCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.SPACES_MISMATCH

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val base = params.baseText ?: return emptyList()
    if (base.isBlank()) return emptyList()
    val text = params.text
    if (text.isBlank()) return emptyList()

    val results = mutableListOf<QaCheckResult>()

    checkLeadingSpaces(base, text, results)
    checkTrailingSpaces(base, text, results)
    checkDoubledSpaces(text, results)
    checkNonBreakingSpaces(base, text, results)

    return results
  }

  private fun checkLeadingSpaces(
    base: String,
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val baseLeading = base.length - base.trimStart(' ', '\t').length
    val textLeading = text.length - text.trimStart(' ', '\t').length

    if (textLeading > baseLeading) {
      val extra = textLeading - baseLeading
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_LEADING_ADDED,
          replacement = "",
          positionStart = 0,
          positionEnd = extra,
        ),
      )
    } else if (textLeading < baseLeading) {
      val missing = base.substring(0, baseLeading)
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_LEADING_REMOVED,
          replacement = missing,
          positionStart = 0,
          positionEnd = 0,
        ),
      )
    }
  }

  private fun checkTrailingSpaces(
    base: String,
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val baseTrailing = base.length - base.trimEnd(' ', '\t').length
    val textTrailing = text.length - text.trimEnd(' ', '\t').length

    if (textTrailing > baseTrailing) {
      val extra = textTrailing - baseTrailing
      val start = text.length - extra
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_TRAILING_ADDED,
          replacement = "",
          positionStart = start,
          positionEnd = text.length,
        ),
      )
    } else if (textTrailing < baseTrailing) {
      val missing = base.substring(base.length - baseTrailing)
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_TRAILING_REMOVED,
          replacement = missing,
          positionStart = text.length,
          positionEnd = text.length,
        ),
      )
    }
  }

  private fun checkDoubledSpaces(
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val interiorStart = text.length - text.trimStart(' ', '\t').length
    val interiorEnd = text.trimEnd(' ', '\t').length
    if (interiorStart >= interiorEnd) return

    val interior = text.substring(interiorStart, interiorEnd)
    val regex = Regex("[ ]{2,}")
    for (match in regex.findAll(interior)) {
      val absStart = interiorStart + match.range.first
      val absEnd = interiorStart + match.range.last + 1
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_DOUBLED,
          replacement = "",
          positionStart = absStart + 1,
          positionEnd = absEnd,
        ),
      )
    }
  }

  private fun checkNonBreakingSpaces(
    base: String,
    text: String,
    results: MutableList<QaCheckResult>,
  ) {
    val nbsp = '\u00A0'
    val baseHasNbsp = nbsp in base
    val textHasNbsp = nbsp in text

    if (!baseHasNbsp && textHasNbsp) {
      for (i in text.indices) {
        if (text[i] == nbsp) {
          results.add(
            QaCheckResult(
              type = QaCheckType.SPACES_MISMATCH,
              message = QaIssueMessage.QA_SPACES_NON_BREAKING_ADDED,
              replacement = " ",
              positionStart = i,
              positionEnd = i + 1,
            ),
          )
        }
      }
    } else if (baseHasNbsp && !textHasNbsp) {
      results.add(
        QaCheckResult(
          type = QaCheckType.SPACES_MISMATCH,
          message = QaIssueMessage.QA_SPACES_NON_BREAKING_REMOVED,
          replacement = null,
          positionStart = 0,
          positionEnd = 0,
        ),
      )
    }
  }
}
