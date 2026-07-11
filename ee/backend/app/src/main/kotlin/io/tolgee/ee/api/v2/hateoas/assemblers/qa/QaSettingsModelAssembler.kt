package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.hateoas.model.qa.QaSettingsModel
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.stereotype.Component

@Component
class QaSettingsModelAssembler {
  fun toModel(settings: Map<QaCheckType, QaCheckSeverity>): QaSettingsModel {
    return QaSettingsModel(settings = settings)
  }
}
