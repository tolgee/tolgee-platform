package io.tolgee.util

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageInputStream
import kotlin.math.sqrt

abstract class ImageProcessor(
  protected val imageStream: InputStream,
) {
  private var imageInput: ImageInputStream? = null
  private var imageReader: ImageReader? = null

  private val reader: ImageReader by lazy {
    closingOnFailure {
      val stream = ImageIO.createImageInputStream(imageStream) ?: throw BadRequestException(Message.FILE_NOT_IMAGE)
      imageInput = stream
      val readers = ImageIO.getImageReaders(stream)
      if (!readers.hasNext()) throw BadRequestException(Message.FILE_NOT_IMAGE)
      readers.next().also {
        it.setInput(stream, true, true)
        imageReader = it
      }
    }
  }

  val originalDimension: Dimension by lazy {
    closingOnFailure {
      val dimension = Dimension(reader.getWidth(0), reader.getHeight(0))
      val sideTooLong = dimension.width > MAX_SOURCE_SIDE || dimension.height > MAX_SOURCE_SIDE
      if (sideTooLong || dimension.width.toLong() * dimension.height > MAX_SOURCE_PIXELS) {
        throw BadRequestException(Message.IMAGE_TOO_LARGE)
      }
      dimension
    }
  }

  protected val sourceBufferedImage: BufferedImage by lazy {
    try {
      val pixels = originalDimension.width.toLong() * originalDimension.height
      val step = sqrt(pixels.toDouble() / MAX_STORED_PIXELS).toInt().coerceAtLeast(1)
      val params = reader.defaultReadParam.apply { setSourceSubsampling(step, step, 0, 0) }
      reader.read(0, params)
    } finally {
      close()
    }
  }

  private fun <T> closingOnFailure(block: () -> T): T = runCatching(block).onFailure { close() }.getOrThrow()

  private fun close() {
    imageReader?.dispose()
    imageReader = null
    imageInput?.close()
    imageInput = null
  }

  protected fun writeImage(
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

  protected fun getWriterParams(
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

  protected fun getWriter() = ImageIO.getImageWritersByFormatName("png").next() as ImageWriter

  companion object {
    const val MAX_STORED_PIXELS = 3_000_000
    const val MAX_SOURCE_SIDE = 50_000
    const val MAX_SOURCE_PIXELS = 100_000_000L
  }
}
