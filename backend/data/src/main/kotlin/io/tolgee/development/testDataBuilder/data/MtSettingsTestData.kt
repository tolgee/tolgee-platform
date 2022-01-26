package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language

class MtSettingsTestData : BaseTestData() {
  var germanLanguage: Language
  var spanishLanguage: Language

  init {
    projectBuilder.apply {
      germanLanguage = addLanguage {
        self {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }
      }.self
      spanishLanguage = addLanguage {
        self {
          name = "Sp"
          tag = "es"
          originalName = "Spanish"
        }
      }.self
    }

    projectBuilder.addMtServiceConfig {
      self {
        targetLanguage = germanLanguage
        enabledServices = mutableSetOf(MtServiceType.AWS)
        primaryService = MtServiceType.AWS
      }
    }

    projectBuilder.addMtServiceConfig {
      self {
        targetLanguage = spanishLanguage
        enabledServices = mutableSetOf(MtServiceType.AWS, MtServiceType.GOOGLE)
        primaryService = MtServiceType.AWS
      }
    }
  }
}
