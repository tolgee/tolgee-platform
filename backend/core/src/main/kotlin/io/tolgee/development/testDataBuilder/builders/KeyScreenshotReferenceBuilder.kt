package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference

class KeyScreenshotReferenceBuilder(
  projectBuilder: ProjectBuilder,
) : EntityDataBuilder<KeyScreenshotReference, KeyScreenshotReferenceBuilder> {
  override var self: KeyScreenshotReference = KeyScreenshotReference()
}
