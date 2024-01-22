package io.tolgee.model.key.screenshotReference

import java.io.Serializable

data class KeyScreenshotReferenceId(
  var key: Long? = null,
  var screenshot: Long? = null,
) : Serializable
