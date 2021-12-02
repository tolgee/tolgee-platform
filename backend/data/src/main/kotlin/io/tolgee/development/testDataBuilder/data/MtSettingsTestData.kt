package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language

class MtSettingsTestData : BaseTestData() {
  var germanLanguage: Language

  init {
    projectBuilder.apply {
      germanLanguage = addLanguage {
        self {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }
      }.self
    }

    projectBuilder.addMtServiceConfig {
      self {
        targetLanguage = germanLanguage
        enabledServices = setOf(MtServiceType.AWS)
        primaryService = MtServiceType.AWS
      }
    }
  }
}
