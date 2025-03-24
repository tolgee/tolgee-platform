package io.tolgee.util

import io.tolgee.model.Screenshot
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import kotlin.math.floor
import kotlin.math.sqrt

class ImageConverter(
  private val imageStream: InputStream,
) {
  private val sourceBufferedImage: BufferedImage by lazy {
    ImageIO.read(imageStream)
  }

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

  private fun writeImage(
    bufferedImage: BufferedImage,
    compressionQuality: Float,
  ): ByteArrayOutputStream {
    val imageWriter = getWriter()
    val writerParams = getWriterParams(imageWriter, compressionQuality)
    val outputStream = ByteArrayOutputStream()
    val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
    imageWriter.output = imageOutputStream
    imageWriter.write(null, IIOImage(bufferedImage, null, null), writerParams)
    outputStream.close()
    imageOutputStream.close()
    imageWriter.dispose()
    return outputStream
  }

  private fun getWriterParams(
    writer: ImageWriter,
    compressionQuality: Float,
  ): ImageWriteParam? {
    val writerParams = writer.defaultWriteParam
    if (compressionQuality > 0) {
      writerParams.compressionMode = ImageWriteParam.MODE_EXPLICIT
      writerParams.compressionQuality = compressionQuality
    }
    return writerParams
  }

  private fun getScaledImage(targetDimension: Dimension): BufferedImage {
    val resized = BufferedImage(targetDimension.width, targetDimension.height, sourceBufferedImage.type)
    val g = resized.createGraphics()
    g.setRenderingHint(
      RenderingHints.KEY_INTERPOLATION,
      RenderingHints.VALUE_INTERPOLATION_BILINEAR,
    )
    g.drawImage(
      sourceBufferedImage, 0, 0, targetDimension.width, targetDimension.height, 0, 0, sourceBufferedImage.width,
      sourceBufferedImage.height, null,
    )
    g.dispose()
    return resized
  }

  private fun getWriter() = ImageIO.getImageWritersByFormatName("png").next() as ImageWriter

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

  private fun convertToBufferedImage(
    img: Image,
    width: Int,
    height: Int,
  ): BufferedImage {
    if (img is BufferedImage) {
      return img
    }

    val bi =
      BufferedImage(
        width,
        height,
        BufferedImage.TYPE_INT_ARGB,
      )
    val graphics2D = bi.createGraphics()
    graphics2D.drawImage(img, 0, 0, null)
    graphics2D.dispose()
    return bi
  }
}
