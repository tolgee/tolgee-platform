package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MachineTranslationServiceType
import io.tolgee.model.Language

class MachineTranslationSettingsTestData : BaseTestData() {
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

    projectBuilder.addProjectTranslationServiceConfig {
      self {
        targetLanguage = germanLanguage
        enabledServices = setOf(MachineTranslationServiceType.AWS)
        primaryService = MachineTranslationServiceType.AWS
      }
    }
  }
}
