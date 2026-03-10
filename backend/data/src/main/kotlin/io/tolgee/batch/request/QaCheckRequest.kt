package io.tolgee.batch.request

import io.tolgee.model.enums.qa.QaCheckType

data class QaCheckRequest(
  val translationIds: List<Long>,
  val checkTypes: List<QaCheckType>? = null,
)
