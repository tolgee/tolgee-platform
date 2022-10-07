package io.tolgee.development.testDataBuilder.data

class NamespacesTestData : BaseTestData() {
  init {
    addKey("key", null)
    addKey("key", "ns-1")
    addKey("key", "ns-2")
    addKey("key2", null)
    addKey("key2", "ns-1")
  }

  private fun addKey(keyName: String, namespace: String?) {
    this.projectBuilder.addKey {
      name = keyName
    }.build {
      setNamespace(namespace)
      addTranslation {
        language = englishLanguage
        text = "hello"
      }
    }
  }
}
