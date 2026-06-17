package io.tolgee.unit

import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.fixtures.undecodableImageBytes
import io.tolgee.util.ImageConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.ImageIO

class ImageConverterValidationTest {
  @Test
  fun `throws FILE_NOT_IMAGE when bytes are undecodable (ImageIO returns null)`() {
    val converter = ImageConverter(ByteArrayInputStream(undecodableImageBytes))
    assertThrows<ValidationException> { converter.originalDimension }
  }

  @Test
  fun `throws FILE_NOT_IMAGE when the image stream throws IOException`() {
    val throwing =
      object : InputStream() {
        override fun read(): Int = throw IOException("boom")
      }
    val converter = ImageConverter(throwing)
    assertThrows<ValidationException> { converter.originalDimension }
  }

  @Test
  fun `degenerate thumbnail aspect does not produce a zero dimension`() {
    val converter = ImageConverter(ByteArrayInputStream(pngBytes(201, 1)))
    assertDoesNotThrow { converter.getThumbnail(200) }
    assertDoesNotThrow { ImageConverter(ByteArrayInputStream(pngBytes(1, 201))).getThumbnail(200) }
  }

  @Test
  fun `degenerate over-3M-px aspect does not produce a zero dimension`() {
    val converter = ImageConverter(ByteArrayInputStream(pngBytes(3_000_001, 1)))
    assertDoesNotThrow { converter.getImage() }
  }

  private fun pngBytes(
    width: Int,
    height: Int,
  ): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val outputStream = ByteArrayOutputStream()
    ImageIO.write(image, "png", outputStream)
    return outputStream.toByteArray()
  }
}
