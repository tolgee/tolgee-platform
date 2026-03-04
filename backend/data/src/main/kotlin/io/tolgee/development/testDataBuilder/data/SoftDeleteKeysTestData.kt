package io.tolgee.development.testDataBuilder.data

class SoftDeleteKeysTestData :
  BaseTestData(
    userName = "soft-delete-keys-user",
    projectName = "Soft delete test",
  ) {
  val czechLanguage = projectBuilder.addCzech().self

  init {
    projectBuilder.apply {
      addKey {
        name = "key1"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Key 1 translation"
        }
      }

      addKey {
        name = "key2"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "Key 2 translation"
        }
      }
    }
  }
}
