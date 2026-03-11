package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckType

data class QaCheckPreviewResult(
  val type: String = "result",
  val checkType: QaCheckType,
  val issues: List<QaPreviewWsIssue>,
)
