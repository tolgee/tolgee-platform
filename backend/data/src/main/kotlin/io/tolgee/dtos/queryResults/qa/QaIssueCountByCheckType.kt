package io.tolgee.dtos.queryResults.qa

import io.tolgee.model.enums.qa.QaCheckType

interface QaIssueCountByCheckType {
  val checkType: QaCheckType
  val count: Long
}
