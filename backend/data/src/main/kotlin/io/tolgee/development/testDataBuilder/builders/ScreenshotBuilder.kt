package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Screenshot

class ScreenshotBuilder(
  keyBuilder: KeyBuilder
) : EntityDataBuilder<Screenshot, ScreenshotBuilder> {
  override var self: Screenshot = Screenshot().also {
    it.key = keyBuilder.self
    keyBuilder.self {
      screenshots.add(it)
    }
  }
}
