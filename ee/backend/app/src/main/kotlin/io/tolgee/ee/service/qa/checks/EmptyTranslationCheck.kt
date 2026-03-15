package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class EmptyTranslationCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.EMPTY_TRANSLATION

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    if (params.text.isBlank()) {
      return listOf(
        QaCheckResult(
          type = QaCheckType.EMPTY_TRANSLATION,
          message = QaIssueMessage.QA_EMPTY_TRANSLATION,
          replacement = null,
          positionStart = 0,
          positionEnd = 0,
        ),
      )
    }
    return emptyList()
  }
}
