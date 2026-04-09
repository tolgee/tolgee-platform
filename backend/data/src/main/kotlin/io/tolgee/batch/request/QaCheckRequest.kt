package io.tolgee.batch.request

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.model.enums.qa.QaCheckType

data class QaCheckRequest(
  val target: List<BatchTranslationTargetItem>,
  val checkTypes: List<QaCheckType>? = null,
)
