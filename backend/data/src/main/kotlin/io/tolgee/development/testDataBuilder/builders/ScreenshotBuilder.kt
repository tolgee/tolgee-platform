package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.Screenshot
import java.io.ByteArrayOutputStream

class ScreenshotBuilder(
  projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Screenshot, ScreenshotBuilder> {
  var thumbnail: ByteArrayOutputStream? = null
  var middleSized: ByteArrayOutputStream? = null
  var image: ByteArrayOutputStream? = null

  override var self: Screenshot = Screenshot()
}
