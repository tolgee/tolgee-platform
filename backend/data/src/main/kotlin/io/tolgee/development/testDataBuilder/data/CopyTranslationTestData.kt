package io.tolgee.development.testDataBuilder.data

class CopyTranslationTestData :
  BaseTestData(
    userName = "copy-translation-user",
    projectName = "Copy translation test",
  ) {
  val czechLanguage = projectBuilder.addCzech().self

  init {
    projectBuilder.apply {
      addKey("Test key").build {
        addTranslation {
          language = englishLanguage
          text = "Translated test key"
        }
      }

      addKey("Plural key").build {
        self.isPlural = true
        self.pluralArgName = "value"
        addTranslation {
          language = englishLanguage
          text =
            "{value, plural,\n" +
            "one {# dog}\n" +
            "other {# dogs}\n" +
            "}"
        }
      }
    }
  }
}
