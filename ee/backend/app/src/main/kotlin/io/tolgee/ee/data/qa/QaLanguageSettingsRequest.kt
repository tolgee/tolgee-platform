package io.tolgee.ee.data.qa

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType

class QaLanguageSettingsRequest {
  @field:Schema(
    description = "Map of check types to their severity. Null values mean 'inherit from global settings'.",
  )
  val settings: Map<QaCheckType, QaCheckSeverity?> = emptyMap()
}
