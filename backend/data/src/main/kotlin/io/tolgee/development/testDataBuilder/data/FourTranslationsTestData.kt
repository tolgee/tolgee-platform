package io.tolgee.development.testDataBuilder.data

class FourTranslationsTestData :
  BaseTestData(
    userName = "four-translations-user",
    projectName = "Four translations test",
  ) {
  val czechLanguage = projectBuilder.addCzech().self

  init {
    projectBuilder.apply {
      (1..4).forEach { index ->
        addKey("Cool key ${index.toString().padStart(2, '0')}").build {
          addTranslation {
            language = englishLanguage
            text = "Cool translated text $index"
          }
          addTranslation {
            language = czechLanguage
            text = "Studený přeložený text $index"
          }
        }
      }
    }
  }
}
