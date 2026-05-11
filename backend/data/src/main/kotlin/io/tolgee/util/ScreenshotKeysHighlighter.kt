package io.tolgee.util

import io.tolgee.model.Screenshot
import java.awt.BasicStroke
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ScreenshotKeysHighlighter(
  imageStream: InputStream,
) : ImageProcessor(imageStream) {
  fun highlightKey(
    screenshot: Screenshot,
    keyId: Long,
  ): ByteArrayOutputStream {
    try {
      val hasPositions =
        screenshot.keyScreenshotReferences
          .any { it.key.id == keyId && !it.positions.isNullOrEmpty() }

      if (!hasPositions) {
        return writeImage(sourceBufferedImage, 0.8f)
      }
      // Load the image
      val newImage = sourceBufferedImage
      val g = newImage.createGraphics()

      val scaling = sourceBufferedImage.width.toFloat() / screenshot.width.toFloat()

      // Set up the red frame properties
      g.apply {
        color = Color.RED
        stroke = BasicStroke(5f)
      }

      screenshot.keyScreenshotReferences.forEach { reference ->
        if (reference.key.id == keyId) {
          reference.positions?.forEach {
            g.drawRect(
              (it.x * scaling).toInt(),
              (it.y * scaling).toInt(),
              (it.width * scaling).toInt(),
              (it.height * scaling).toInt(),
            )
          }
        }
      }

      g.dispose()
      return writeImage(newImage, 0.8f)
    } catch (e: Exception) {
      throw RuntimeException("Error processing image: ${e.message}")
    }
  }
}
