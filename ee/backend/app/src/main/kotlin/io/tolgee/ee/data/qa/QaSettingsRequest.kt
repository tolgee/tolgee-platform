package io.tolgee.ee.data.qa

import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType

class QaSettingsRequest {
  val settings: Map<QaCheckType, QaCheckSeverity> = emptyMap()
}
