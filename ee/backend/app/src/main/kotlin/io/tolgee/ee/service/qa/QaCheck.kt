package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType

interface QaCheck {
  val type: QaCheckType

  fun check(params: QaCheckParams): List<QaCheckResult>
}
