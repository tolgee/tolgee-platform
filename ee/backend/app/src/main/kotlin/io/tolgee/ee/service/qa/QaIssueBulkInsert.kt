package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaIssueState

data class QaIssueBulkInsert(
  val translationId: Long,
  val result: QaCheckResult,
  val state: QaIssueState,
)
