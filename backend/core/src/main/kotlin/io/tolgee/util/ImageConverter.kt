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
  val originalDimension: Dimension by lazy {
    Dimension(sourceBufferedImage.width, sourceBufferedImage.height)
  }

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
    val resized = BufferedImage(targetDimension.width, targetDimension.height, sourceBufferedImage.type)
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

  val targetDimension: Dimension by lazy {
    val imagePxs = sourceBufferedImage.height * sourceBufferedImage.width
    val maxPxs = 3000000
    val newHeight = floor(sqrt(maxPxs.toDouble() * sourceBufferedImage.height / sourceBufferedImage.width)).toInt()
    val newWidth = floor(sqrt(maxPxs.toDouble() * sourceBufferedImage.width / sourceBufferedImage.height)).toInt()

    if (imagePxs > maxPxs) {
      return@lazy Dimension(newWidth, newHeight)
    }
    return@lazy Dimension(sourceBufferedImage.width, sourceBufferedImage.height)
  }
}
