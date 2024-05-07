package io.tolgee.development.testDataBuilder.data

class SingleStepImportTestData : BaseTestData() {
  val germanLanguage = projectBuilder.addGerman()

  fun addConflictTranslation() {
    val key =
      projectBuilder.addKey {
        this.name = "test"
      }
    projectBuilder.addTranslation {
      this.key = key.self
      this.text = "conflict!"
      this.language = englishLanguage
    }
  }
}
