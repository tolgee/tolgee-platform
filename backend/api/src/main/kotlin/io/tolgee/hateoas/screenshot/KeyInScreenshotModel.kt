package io.tolgee.hateoas.screenshot

import io.tolgee.model.key.screenshotReference.KeyInScreenshotPosition

class KeyInScreenshotModel(
  val keyId: Long,
  val position: KeyInScreenshotPosition?,
  val keyName: String,
  val keyNamespace: String?,
  val originalText: String?,
)
