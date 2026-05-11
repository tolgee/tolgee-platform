package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import io.tolgee.service.translation.getMaxVisibleCharCount
import org.springframework.stereotype.Component

@Component
class KeyLengthLimitCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.KEY_LENGTH_LIMIT

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val limit = params.maxCharLimit ?: return emptyList()
    if (limit <= 0) return emptyList()

    val count = getMaxVisibleCharCount(params.text, params.isPlural)
    if (count <= limit) return emptyList()

    return listOf(
      QaCheckResult(
        type = QaCheckType.KEY_LENGTH_LIMIT,
        message = QaIssueMessage.QA_KEY_LENGTH_LIMIT_EXCEEDED,
        replacement = null,
        positionStart = null,
        positionEnd = null,
        params = mapOf("limit" to limit.toString(), "count" to count.toString()),
      ),
    )
  }
}
