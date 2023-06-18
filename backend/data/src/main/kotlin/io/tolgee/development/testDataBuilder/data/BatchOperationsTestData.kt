package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key

class BatchOperationsTestData : BaseTestData() {
  fun addTranslationOperationData(keyCount: Int = 100): List<Key> {
    this.projectBuilder.addCzech()
    this.projectBuilder.addGerman()

    return (1..keyCount).map {
      this.projectBuilder.addKey {
        name = "key$it"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "en$it"
        }
      }.self
    }
  }
}
