package io.tolgee.ee.data.qa

import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue

data class QaPreviewWsIssue(
  val type: QaCheckType,
  val message: QaIssueMessage,
  val replacement: String?,
  val positionStart: Int?,
  val positionEnd: Int?,
  val params: Map<String, String>?,
  val state: QaIssueState,
  val pluralVariant: String? = null,
) {
  companion object {
    fun fromQaCheckResult(
      result: QaCheckResult,
      persistedIssues: List<TranslationQaIssue>,
    ): QaPreviewWsIssue {
      val matchingIssue =
        persistedIssues.find { issue ->
          issue.type == result.type &&
            issue.message == result.message &&
            issue.replacement == result.replacement &&
            issue.positionStart == result.positionStart &&
            issue.positionEnd == result.positionEnd &&
            issue.pluralVariant == result.pluralVariant
        }
      return QaPreviewWsIssue(
        type = result.type,
        message = result.message,
        replacement = result.replacement,
        positionStart = result.positionStart,
        positionEnd = result.positionEnd,
        params = result.params,
        state = matchingIssue?.state ?: QaIssueState.OPEN,
        pluralVariant = result.pluralVariant,
      )
    }
  }
}
