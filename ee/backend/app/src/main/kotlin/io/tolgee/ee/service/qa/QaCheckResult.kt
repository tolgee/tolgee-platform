package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage

data class QaCheckResult(
  val type: QaCheckType,
  val message: QaIssueMessage,
  val replacement: String?,
  val positionStart: Int,
  val positionEnd: Int,
  val params: Map<String, String>? = null,
)
