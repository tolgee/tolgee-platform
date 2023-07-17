package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key

class BatchJobsTestData : BaseTestData() {

  val anotherUser = root.addUserAccount { username = "anotherUser" }.self
  val germanLanguage = projectBuilder.addGerman().self
  val czechLanguage = projectBuilder.addCzech().self

  init {
    this.projectBuilder.addPermission {
      user = anotherUser
      scopes = arrayOf(Scope.KEYS_VIEW)
    }
  }

  fun addTranslationOperationData(keyCount: Int = 100): List<Key> {
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

  fun addStateChangeData(keyCount: Int = 100): List<Key> {
    return (1..keyCount).map {
      this.projectBuilder.addKey {
        name = "key$it"
      }.build {
        addTranslation {
          language = englishLanguage
          text = "en$it"
        }
        addTranslation {
          language = germanLanguage
          text = "de$it"
        }
        addTranslation {
          language = czechLanguage
          text = "cs$it"
        }
      }.self
    }
  }
}
