package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

class QaAutoTranslateRaceTestData : BaseTestData() {
  lateinit var raceKey: Key
  lateinit var autoTranslations: List<Translation>

  init {
    project.useQaChecks = true
    projectBuilder.apply {
      val targetLanguages =
        listOf("de" to "German", "es" to "Spanish", "fr" to "French").map { (languageTag, languageName) ->
          addLanguage {
            tag = languageTag
            name = languageName
          }.self
        }
      addAutoTranslationConfig {
        usingTm = true
        usingPrimaryMtService = true
      }
      raceKey =
        addKey { name = "race-key" }
          .build {
            addTranslation("en", OLD_BASE)
            autoTranslations =
              targetLanguages.map { targetLanguage ->
                addTranslation {
                  language = targetLanguage
                  text = "old ${targetLanguage.tag}"
                  auto = true
                }.self
              }
          }.self
    }
  }

  companion object {
    const val OLD_BASE = "Old base"
  }
}
