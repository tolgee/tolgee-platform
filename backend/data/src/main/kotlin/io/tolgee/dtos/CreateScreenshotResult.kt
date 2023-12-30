package io.tolgee.dtos

import io.tolgee.model.Screenshot
import java.awt.Dimension

data class CreateScreenshotResult(
  val screenshot: Screenshot,
  val originalDimension: Dimension,
  val targetDimension: Dimension,
)
