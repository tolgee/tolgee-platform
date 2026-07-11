package io.tolgee.model.batch.params

import io.tolgee.model.enums.qa.QaCheckType

class QaCheckJobParams {
  /**
   * null -> all checks
   */
  var checkTypes: List<QaCheckType>? = null
  var handlingStuckStaleItems: Boolean = false
}
