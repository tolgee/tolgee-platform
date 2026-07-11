package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType

interface QaCheck {
  val type: QaCheckType

  // if null, debounce is not used
  val debounceDuration: Long?
    get() = null

  fun check(params: QaCheckParams): List<QaCheckResult>
}
