package io.tolgee.ee.api.v2.hateoas.assemblers.qa

import io.tolgee.ee.api.v2.hateoas.model.qa.QaLanguageSettingsModel
import io.tolgee.model.enums.qa.QaCheckSeverity
import io.tolgee.model.enums.qa.QaCheckType
import org.springframework.stereotype.Component

@Component
class QaLanguageSettingsModelAssembler {
  fun toModel(settings: Map<QaCheckType, QaCheckSeverity>?): QaLanguageSettingsModel {
    return QaLanguageSettingsModel(settings = settings)
  }
}
