package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.key.Key
import org.springframework.core.io.ClassPathResource

class GuestScreenshotTestData : BaseTestData() {
  val key: Key

  init {
    projectBuilder.self.public = true
    val keyBuilder = projectBuilder.addKey { name = "test" }
    keyBuilder.addScreenshot(ClassPathResource("development/testScreenshot.png", this::class.java.classLoader)) {}
    key = keyBuilder.self
  }
}
