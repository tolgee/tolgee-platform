package io.tolgee.util

import org.springframework.core.io.InputStreamSource
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun generateImage(
  width: Int = 2000,
  height: Int = 3000,
): InputStreamSource {
  val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
  image.createGraphics().drawString("Hello World", 10, 20)
  val outputStream = ByteArrayOutputStream()
  ImageIO.write(image, "jpg", outputStream)
  return InputStreamSource { outputStream.toByteArray().inputStream() }
}
