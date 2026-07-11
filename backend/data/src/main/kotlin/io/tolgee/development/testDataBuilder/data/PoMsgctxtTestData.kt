package io.tolgee.development.testDataBuilder.data

class PoMsgctxtTestData : BaseTestData() {
  init {
    projectBuilder.apply {
      addKey {
        name = "plain.key"
      }.build {
        addTranslation("en", "Plain key without msgctxt")
      }

      addKey {
        name = "menu\u0004Open"
      }.build {
        addTranslation("en", "Open file from the menu")
      }
    }
  }
}
