package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckType

data class QaCheckPreviewResult(
  val type: QaPreviewMessageType = QaPreviewMessageType.RESULT,
  val checkType: QaCheckType,
  val issues: List<QaPreviewWsIssue>,
)
