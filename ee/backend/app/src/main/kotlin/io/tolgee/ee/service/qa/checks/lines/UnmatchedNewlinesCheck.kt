package io.tolgee.ee.service.qa.checks.lines

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaPluralCheckHelper
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class UnmatchedNewlinesCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.UNMATCHED_NEWLINES

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    return QaPluralCheckHelper.runPerVariant(params) { text, baseText ->
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

    val baseStructure = extractStructure(base)
    val textStructure = extractStructure(text)

    if (baseStructure.gaps.size != textStructure.gaps.size) {
      return listOf(structureMismatchResult(baseStructure.gaps.size - 1, textStructure.gaps.size - 1))
    }

    val results = mutableListOf<QaCheckResult>()
    for (i in baseStructure.gaps.indices) {
      val baseGap = baseStructure.gaps[i]
      val textGap = textStructure.gaps[i]

      if (textGap.lineCount > baseGap.lineCount) {
        results.add(extraNewlinesResult(baseGap, textGap, textStructure.separatorType))
        continue
      }

      if (textGap.lineCount < baseGap.lineCount) {
        results.add(missingNewlinesResult(baseGap, textGap, textStructure.separatorType))
        continue
      }
    }

    return results
  }

  private fun structureMismatchResult(
    baseBlocks: Int,
    textBlocks: Int,
  ): QaCheckResult {
    val message =
      if (textBlocks > baseBlocks) {
        QaIssueMessage.QA_NEWLINES_TOO_MANY_SECTIONS
      } else {
        QaIssueMessage.QA_NEWLINES_TOO_FEW_SECTIONS
      }
    return QaCheckResult(
      type = QaCheckType.UNMATCHED_NEWLINES,
      message = message,
      replacement = null,
      positionStart = null,
      positionEnd = null,
      params = mapOf("expected" to baseBlocks.toString(), "actual" to textBlocks.toString()),
    )
  }

  private fun extraNewlinesResult(
    baseGap: Gap,
    textGap: Gap,
    separatorType: SeparatorType,
  ): QaCheckResult {
    val diff = textGap.lineCount - baseGap.lineCount
    val charCount = diff * separatorType.separator.length
    return QaCheckResult(
      type = QaCheckType.UNMATCHED_NEWLINES,
      message = QaIssueMessage.QA_NEWLINES_EXTRA,
      replacement = "",
      positionStart = textGap.endIndex - charCount,
      positionEnd = textGap.endIndex,
      params = mapOf("count" to diff.toString()),
    )
  }

  private fun missingNewlinesResult(
    baseGap: Gap,
    textGap: Gap,
    separatorType: SeparatorType,
  ): QaCheckResult {
    val missing = baseGap.lineCount - textGap.lineCount
    return QaCheckResult(
      type = QaCheckType.UNMATCHED_NEWLINES,
      message = QaIssueMessage.QA_NEWLINES_MISSING,
      replacement = separatorType.separator.repeat(missing),
      positionStart = textGap.endIndex,
      positionEnd = textGap.endIndex,
      params = mapOf("count" to missing.toString()),
    )
  }
}
