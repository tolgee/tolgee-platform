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
import kotlin.math.min
import kotlin.math.sqrt

class ImageConverter(
  private val imageStream: InputStream,
) {
  private val sourceBufferedImage: BufferedImage by lazy {
    ImageIO.read(imageStream)
  }

  fun getImage(compressionQuality: Float = 0.5f, targetDimension: Dimension? = null): ByteArrayOutputStream {
    val resizedImage = getScaledImage(targetDimension)
    val bufferedImage = convertToBufferedImage(resizedImage)
    return writeImage(bufferedImage, compressionQuality)
  }

  fun getThumbNail(size: Int = 150, compressionQuality: Float = 0.5f): ByteArrayOutputStream {
    val side = min(sourceBufferedImage.width, sourceBufferedImage.height)
    val x = (sourceBufferedImage.width - side) / 2
    val y = (sourceBufferedImage.height - side) / 2
    val cropped = sourceBufferedImage.getSubimage(x, y, side, side)
    val resizedImage = cropped.getScaledInstance(size, size, Image.SCALE_SMOOTH)
    val bufferedImage = convertToBufferedImage(resizedImage)
    return writeImage(bufferedImage, compressionQuality)
  }

  private fun writeImage(bufferedImage: BufferedImage, compressionQuality: Float): ByteArrayOutputStream {
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

  private fun getWriterParams(writer: ImageWriter, compressionQuality: Float): ImageWriteParam? {
    val writerParams = writer.defaultWriteParam
    if (compressionQuality > 0) {
      writerParams.compressionMode = ImageWriteParam.MODE_EXPLICIT
      writerParams.compressionQuality = compressionQuality
    }
    return writerParams
  }

  private fun getScaledImage(targetDimension: Dimension?): Image {
    val resultingTargetDimension = targetDimension ?: getTargetDimension()
    return sourceBufferedImage.getScaledInstance(
      resultingTargetDimension.width,
      resultingTargetDimension.height,
      Image.SCALE_SMOOTH
    )
  }

  private fun getWriter() = ImageIO.getImageWritersByFormatName("png").next() as ImageWriter

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

  private fun convertToBufferedImage(img: Image): BufferedImage {
    if (img is BufferedImage) {
      return img
    }
    val bi = BufferedImage(
      img.getWidth(null), img.getHeight(null),
      BufferedImage.TYPE_INT_ARGB
    )
    val graphics2D = bi.createGraphics()
    graphics2D.drawImage(img, 0, 0, null)
    graphics2D.dispose()
    return bi
  }
}
