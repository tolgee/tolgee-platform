package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key

class BatchJobsTestData : BaseTestData() {

  val anotherUser = root.addUserAccount { username = "anotherUser" }.self

  init {
    this.projectBuilder.addPermission {
      user = anotherUser
      scopes = arrayOf(Scope.KEYS_VIEW)
    }
  }

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
