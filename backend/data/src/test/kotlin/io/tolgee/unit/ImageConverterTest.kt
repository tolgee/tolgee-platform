package io.tolgee.unit

import com.sun.management.ThreadMXBean
import io.tolgee.exceptions.BadRequestException
import io.tolgee.testing.assert
import io.tolgee.util.ImageConverter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Dimension
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.lang.management.ManagementFactory
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import javax.imageio.ImageIO

class ImageConverterTest {
  @Test
  fun `shrinks a huge image without holding all its pixels in memory`() {
    val converter = ImageConverter(solidPng(8000, 8000).inputStream())
    val thread = ManagementFactory.getThreadMXBean() as ThreadMXBean
    val allocatedBefore = thread.currentThreadAllocatedBytes

    converter.originalDimension.assert.isEqualTo(Dimension(8000, 8000))
    val stored = converter.getImage().toByteArray()

    val allocatedMb = (thread.currentThreadAllocatedBytes - allocatedBefore) / 1024 / 1024
    allocatedMb.assert.isLessThan(100)
    val storedImage = ImageIO.read(stored.inputStream())
    storedImage.width.assert.isEqualTo(1732)
    storedImage.height.assert.isEqualTo(1732)
  }

  @Test
  fun `rejects an image with too many pixels before decoding it`() {
    val converter = ImageConverter(solidPng(20_000, 20_000, rows = 1).inputStream())
    val exception = assertThrows<BadRequestException> { converter.originalDimension }
    exception.code.assert.isEqualTo("image_too_large")
  }

  @Test
  fun `rejects an image with one side over the limit even when the pixel count is small`() {
    val converter = ImageConverter(solidPng(100_000, 1).inputStream())
    val exception = assertThrows<BadRequestException> { converter.originalDimension }
    exception.code.assert.isEqualTo("image_too_large")
  }

  private fun solidPng(
    width: Int,
    height: Int,
    rows: Int = height,
  ): ByteArray {
    val row = ByteArray(1 + width * 4) { if (it == 0) 0 else 0xFF.toByte() }
    val pixels = ByteArrayOutputStream()
    DeflaterOutputStream(pixels, Deflater(Deflater.BEST_SPEED)).use { out -> repeat(rows) { out.write(row) } }
    val header = ByteArrayOutputStream()
    DataOutputStream(header).apply {
      writeInt(width)
      writeInt(height)
      write(byteArrayOf(8, 6, 0, 0, 0))
    }
    val png = ByteArrayOutputStream()
    png.write(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
    png.writeChunk("IHDR", header.toByteArray())
    png.writeChunk("IDAT", pixels.toByteArray())
    png.writeChunk("IEND", ByteArray(0))
    return png.toByteArray()
  }

  private fun ByteArrayOutputStream.writeChunk(
    type: String,
    data: ByteArray,
  ) {
    val typeBytes = type.toByteArray(Charsets.US_ASCII)
    val crc =
      CRC32().apply {
        update(typeBytes)
        update(data)
      }
    DataOutputStream(this).apply {
      writeInt(data.size)
      write(typeBytes)
      write(data)
      writeInt(crc.value.toInt())
    }
  }
}
