package io.tolgee.development.testDataBuilder.data

class CharLimitTestData :
  BaseTestData(
    userName = "char-limit-user",
    projectName = "Char limit test",
  ) {
  val czechLanguage = projectBuilder.addCzech().self

  init {
    projectBuilder.apply {
      addKey {
        name = "key-no-limit"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "No limit translation"
        }
      }

      addKey {
        name = "key-with-limit"
        maxCharLimit = 5
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Hi"
        }
      }

      addKey {
        name = "char-limit-key"
        maxCharLimit = 5
      }.build {
        addTranslation {
          language = englishLanguage
          text = "OK"
        }
      }
    }
  }
}
