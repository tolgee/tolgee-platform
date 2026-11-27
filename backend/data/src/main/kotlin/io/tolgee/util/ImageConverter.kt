package io.tolgee.util

import java.awt.Dimension
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.floor
import kotlin.math.sqrt

class ImageConverter(
  imageStream: InputStream,
) : ImageProcessor(imageStream) {
  fun getImage(
    compressionQuality: Float = 0.8f,
    targetDimension: Dimension? = null,
  ): ByteArrayOutputStream {
    val resultingTargetDimension = targetDimension ?: this.targetDimension
    val resizedImage = getScaledImage(resultingTargetDimension)
    return writeImage(resizedImage, compressionQuality)
  }

  fun getThumbnail(
    size: Int = 150,
    compressionQuality: Float = 0.8f,
  ): ByteArrayOutputStream {
    val originalWidth = sourceBufferedImage.width
    val originalHeight = sourceBufferedImage.height
    val newWidth: Int
    val newHeight: Int

    if (originalWidth > originalHeight) {
      newWidth = size
      newHeight = (originalHeight * size) / originalWidth
    } else {
      newHeight = size
      newWidth = (originalWidth * size) / originalHeight
    }

    val resizedImage = getScaledImage(Dimension(newWidth, newHeight))
    return writeImage(resizedImage, compressionQuality)
  }

  private fun getScaledImage(targetDimension: Dimension): BufferedImage {
    val resized = BufferedImage(targetDimension.width, targetDimension.height, getSafeImageType())
    val g = resized.createGraphics()
    g.setRenderingHint(
      RenderingHints.KEY_INTERPOLATION,
      RenderingHints.VALUE_INTERPOLATION_BILINEAR,
    )
    g.drawImage(
      sourceBufferedImage,
      0,
      0,
      targetDimension.width,
      targetDimension.height,
      0,
      0,
      sourceBufferedImage.width,
      sourceBufferedImage.height,
      null,
    )
    g.dispose()
    return resized
  }

  private fun getSafeImageType(): Int {
    if (sourceBufferedImage.type != BufferedImage.TYPE_CUSTOM) {
      return sourceBufferedImage.type
    }
    if (sourceBufferedImage.colorModel.hasAlpha()) {
      return BufferedImage.TYPE_INT_ARGB
    }
    return BufferedImage.TYPE_INT_RGB
  }

  val targetDimension: Dimension by lazy {
    val width = originalDimension.width
    val height = originalDimension.height
    val imagePxs = width.toLong() * height
    val newHeight = floor(sqrt(MAX_STORED_PIXELS.toDouble() * height / width)).toInt()
    val newWidth = floor(sqrt(MAX_STORED_PIXELS.toDouble() * width / height)).toInt()

    if (imagePxs > MAX_STORED_PIXELS) {
      return@lazy Dimension(newWidth, newHeight)
    }
    return@lazy Dimension(width, height)
  }
}
