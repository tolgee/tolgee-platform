package io.tolgee.model.key.screenshotReference

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import java.io.Serializable

class KeyScreenshotReferenceId(
  var key: Key? = null,
  var screenshot: Screenshot? = null
) : Serializable
