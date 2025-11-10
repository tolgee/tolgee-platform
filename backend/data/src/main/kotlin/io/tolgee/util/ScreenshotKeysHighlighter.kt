package io.tolgee.util

import io.tolgee.model.Screenshot
import java.awt.BasicStroke
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ScreenshotKeysHighlighter(
  imageStream: InputStream,
) : ImageProcessor(imageStream) {
  fun highlightKeys(
    screenshot: Screenshot,
    keyIds: List<Long>,
  ): ByteArrayOutputStream {
    try {
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
        if (keyIds.contains(reference.key.id)) {
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
      return writeImage(newImage, 1f)
    } catch (e: Exception) {
      throw RuntimeException("Error processing image: ${e.message}")
    }
  }
}
