package io.tolgee.development.testDataBuilder.data

class ResolvableImportTestData : BaseTestData() {
  init {
    projectBuilder.apply {
      addGerman()

      addKey("namespace-1", "key-1") {
        addTranslation("de", "existing translation")
      }

      addKey("namespace-1", "key-2") {
        addTranslation("en", "existing translation")
      }
    }
  }
}
