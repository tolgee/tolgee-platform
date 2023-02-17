package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Screenshot

class ResolvableImportTestData : BaseTestData() {

  lateinit var key1and2Screenshot: Screenshot
  lateinit var key2Screenshot: Screenshot

  init {
    projectBuilder.apply {
      addGerman()

      addKey("namespace-1", "key-1") {
        addTranslation("de", "existing translation")
        key1and2Screenshot = addScreenshot {
          location = "My cool frame"
        }.self
      }

      addKey("namespace-1", "key-2") {
        addTranslation("en", "existing translation")
        key2Screenshot = addScreenshot {}.self
        addScreenshotReference {
          key = this@addKey.self
          screenshot = key1and2Screenshot
        }
      }
      addKey("test") {
        addTranslation("en", "existing translation")
      }
    }
  }
}
