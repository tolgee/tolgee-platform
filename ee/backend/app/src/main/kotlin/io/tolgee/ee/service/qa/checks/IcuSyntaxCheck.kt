package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheck
import io.tolgee.ee.service.qa.QaCheckParams
import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.formats.MessagePatternUtil
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.springframework.stereotype.Component

@Component
class IcuSyntaxCheck : QaCheck {
  override val type: QaCheckType = QaCheckType.ICU_SYNTAX

  override fun check(params: QaCheckParams): List<QaCheckResult> {
    val text = params.text
    if (text.isEmpty()) return emptyList()

    return try {
      MessagePatternUtil.buildMessageNode(text)
      emptyList()
    } catch (e: Exception) {
      listOf(
        QaCheckResult(
          type = QaCheckType.ICU_SYNTAX,
          message = QaIssueMessage.QA_ICU_SYNTAX_ERROR,
          replacement = null,
          positionStart = 0,
          positionEnd = text.length,
        ),
      )
    }
  }
}
