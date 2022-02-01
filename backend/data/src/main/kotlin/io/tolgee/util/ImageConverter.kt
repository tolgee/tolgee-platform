package io.tolgee.util

import java.awt.Dimension
import java.awt.Image
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
  private val imageStream: InputStream
) {
  lateinit var sourceBufferedImage: BufferedImage
  lateinit var resultBufferedImage: BufferedImage
  lateinit var resultingTargetDimension: Dimension
  lateinit var imageWriter: ImageWriter

  fun prepareImage(
    compressionQuality: Float = 0.5f,
    targetDimension: Dimension? = null,
    format: String = "jpg"
  ): ByteArrayOutputStream {
    sourceBufferedImage = ImageIO.read(imageStream)
    resultingTargetDimension = targetDimension ?: getTargetDimension()
    imageWriter = getWriter(format)
    val resizedImage = getScaledImage()
    resultBufferedImage = getResultBufferedImage(format, resizedImage)
    return writeImage(compressionQuality)
  }

  private fun writeImage(compressionQuality: Float): ByteArrayOutputStream {
    val writerParams = getWriterParams(compressionQuality)
    val outputStream = ByteArrayOutputStream()
    val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
    imageWriter.output = imageOutputStream
    imageWriter.write(null, IIOImage(resultBufferedImage, null, null), writerParams)
    outputStream.close()
    imageOutputStream.close()
    imageWriter.dispose()
    return outputStream
  }

  private fun getWriterParams(compressionQuality: Float): ImageWriteParam? {
    val writerParams = imageWriter.defaultWriteParam
    if (compressionQuality > 0) {
      writerParams.compressionMode = ImageWriteParam.MODE_EXPLICIT
      writerParams.compressionQuality = compressionQuality
    }
    return writerParams
  }

  private fun getResultBufferedImage(format: String, resizedImage: Image): BufferedImage {
    val colorSpace = if (format == "jpg") BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
    val resultBufferedImage = convertToBufferedImage(resizedImage, colorSpace)
    return resultBufferedImage
  }

  private fun getScaledImage() = sourceBufferedImage.getScaledInstance(
    resultingTargetDimension.width,
    resultingTargetDimension.height,
    Image.SCALE_SMOOTH
  )

  private fun getWriter(format: String) = ImageIO.getImageWritersByFormatName(format).next() as ImageWriter

  private fun getTargetDimension(): Dimension {
    val imagePxs = sourceBufferedImage.height * sourceBufferedImage.width
    val maxPxs = 3000000
    val newHeight = floor(sqrt(maxPxs.toDouble() * sourceBufferedImage.height / sourceBufferedImage.width)).toInt()
    val newWidth = floor(sqrt(maxPxs.toDouble() * sourceBufferedImage.width / sourceBufferedImage.height)).toInt()

    if (imagePxs > maxPxs) {
      return Dimension(newWidth, newHeight)
    }
    return Dimension(sourceBufferedImage.width, sourceBufferedImage.height)
  }

  private fun convertToBufferedImage(img: Image, colorSpace: Int): BufferedImage {
    if (img is BufferedImage) {
      return img
    }

    // Create a buffered image with transparency
    val bi = BufferedImage(
      img.getWidth(null), img.getHeight(null),
      colorSpace
    )
    val graphics2D = bi.createGraphics()
    graphics2D.drawImage(img, 0, 0, null)
    graphics2D.dispose()
    return bi
  }
}
