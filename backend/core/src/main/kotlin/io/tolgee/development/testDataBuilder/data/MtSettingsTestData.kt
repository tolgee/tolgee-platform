package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language

class MtSettingsTestData : BaseTestData() {
  var germanLanguage: Language
  var spanishLanguage: Language

  init {
    projectBuilder.apply {
      germanLanguage =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
        }.self
      spanishLanguage =
        addLanguage {
          name = "Sp"
          tag = "es"
          originalName = "Spanish"
        }.self

      addFrench()
    }

    projectBuilder.addMtServiceConfig {
      targetLanguage = germanLanguage
      enabledServices = mutableSetOf(MtServiceType.AWS)
      primaryService = MtServiceType.AWS
    }

    projectBuilder.addMtServiceConfig {
      targetLanguage = spanishLanguage
      enabledServices = mutableSetOf(MtServiceType.AWS, MtServiceType.GOOGLE)
      primaryService = MtServiceType.AWS
    }
    projectBuilder.addMtServiceConfig {
      targetLanguage = null
      enabledServices = mutableSetOf(MtServiceType.AWS, MtServiceType.GOOGLE)
      primaryService = MtServiceType.AWS
    }
  }
}
