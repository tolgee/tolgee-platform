package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.key.Key

class NamespacesTestData : BaseTestData() {
  var keyInNs1: Key
  var singleKeyInNs2: Key

  init {
    projectBuilder.apply {
      addKey("key", null)
      keyInNs1 = addKey("key", "ns-1")
      singleKeyInNs2 = addKey("key", "ns-2")
      addKey("key2", null)
      addKey("key2", "ns-1")
    }
    root.apply {
      addProject {
        name = "Project 2"
      }.build {
        addKey("key", null)
        addKey("key", "ns-1")
      }
    }
  }

  private fun ProjectBuilder.addKey(keyName: String, namespace: String?): Key {
    val keyBuilder = this.addKey {
      name = keyName
    }.build {
      setNamespace(namespace)
      addTranslation {
        language = englishLanguage
        text = "hello"
      }
    }
    return keyBuilder.self
  }
}
