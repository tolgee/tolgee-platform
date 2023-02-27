package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Screenshot

class ScreenshotBuilder(
  projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Screenshot, ScreenshotBuilder> {
  override var self: Screenshot = Screenshot()
}
