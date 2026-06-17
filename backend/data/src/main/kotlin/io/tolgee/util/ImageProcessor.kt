package io.tolgee.util

import io.tolgee.constants.Message
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter

abstract class ImageProcessor(
  protected val imageStream: InputStream,
) {
  protected val sourceBufferedImage: BufferedImage by lazy {
    // ImageIO.read can return null (no reader) OR throw unchecked AIOOBE / NegativeArraySizeException /
    // IllegalArgumentException on malformed bytes — all catches are intentional.
    val decoded =
      try {
        ImageIO.read(imageStream)
      } catch (e: IOException) {
        null
      } catch (e: IllegalArgumentException) {
        null
      } catch (e: ArrayIndexOutOfBoundsException) {
        null
      } catch (e: NegativeArraySizeException) {
        null
      }
    decoded ?: throw ValidationException(Message.FILE_NOT_IMAGE)
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
}
