package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Screenshot

class KeysInfoTestData : BaseTestData() {
  lateinit var key1and2Screenshot: Screenshot
  lateinit var key2Screenshot: Screenshot

  init {
    projectBuilder.apply {
      addGerman()

      addKey("namespace-1", "key-1") {
        setDescription("description")
        addTranslation("en", "existing translation")
        addTranslation("de", "existing translation")
        key1and2Screenshot = addScreenshot { }.self
      }

      val key2 =
        addKey("namespace-1", "key-2") {
          setDescription("description")
          addTranslation("de", "existing translation")
        }

      addScreenshotReference {
        screenshot = key1and2Screenshot
        key = key2.self
      }

      (1..50).forEach {
        addKey("key-$it") {
          setDescription("description")
          addTranslation("de", "existing translation")
          addMeta {
            custom = mutableMapOf("key" to "value")
          }
        }
        addKey("ns", "key-$it") {
          setDescription("description")
          addTranslation("de", "existing translation")
        }
      }
    }
  }
}
