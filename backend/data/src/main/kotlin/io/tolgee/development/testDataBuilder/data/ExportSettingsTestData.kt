package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.TranslationState

class ExportSettingsTestData : BaseTestData("export_settings_user", "Export settings project") {
  init {
    projectBuilder.self.useNamespaces = true
    projectBuilder.apply {
      addCzech()
      addGerman()

      addKey("reviewed-key") {
        addTranslation("en", "Reviewed text").self.state = TranslationState.REVIEWED
        addTranslation("cs", "Zkontrolovaný text").self.state = TranslationState.REVIEWED
      }
      addKey("translated-key") {
        addTranslation("en", "Translated text")
        addTranslation("cs", "Přeložený text")
      }
      addKey("untranslated-in-czech-key") {
        addTranslation("en", "Only English text")
      }
      addKey("web", "web-key") {
        addTranslation("en", "Web text")
      }
      addKey("release-key") {
        addTranslation("en", "Release text")
        addTranslation("cs", "Text vydání")
        addTag("release")
      }
    }
  }
}
