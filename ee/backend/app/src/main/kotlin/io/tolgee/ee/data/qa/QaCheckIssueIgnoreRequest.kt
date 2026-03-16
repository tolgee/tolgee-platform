package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage

data class QaCheckIssueIgnoreRequest(
  val type: QaCheckType,
  val message: QaIssueMessage,
  val replacement: String?,
  val positionStart: Int,
  val positionEnd: Int,
  val params: Map<String, String>? = null,
  val pluralVariant: String? = null,
)
