package io.tolgee.ee.unit

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyInScreenshotPosition
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.util.ImageConverter
import io.tolgee.util.ScreenshotKeysHighlighter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Date

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScreenshotKeysHighlighterTest {
  private val log = LoggerFactory.getLogger(javaClass)

  companion object {
    const val WIDTH = 2000
    const val HEIGHT = 1500

    // Anthropic API rejects base64-decoded images larger than 5 MB
    const val MAX_API_SIZE_BYTES = 5 * 1024 * 1024
  }

  // Generated once — image creation is expensive
  private lateinit var storedBytes: ByteArray

  // Re-created per test so each test gets a clean Screenshot with no references
  private lateinit var key: Key
  private lateinit var screenshot: Screenshot

  @BeforeAll
  fun setUpClass() {
    val original = createRealisticScreenshot(WIDTH, HEIGHT)
    val converter = ImageConverter(original)
    storedBytes = converter.getImage(compressionQuality = 0.8f).toByteArray()
  }

  @BeforeEach
  fun setUp() {
    key = Key().apply { id = 1L }
    screenshot =
      Screenshot().apply {
        id = 1L
        createdAt = Date()
        width = WIDTH
        height = HEIGHT
      }
  }

  @Test
  fun `screenshot without positions should not inflate after re-encoding`() {
    // Reference exists but has NO positions — no rectangles to draw.
    // PromptParamsHelper still routes through ScreenshotKeysHighlighter
    // because the reference exists (line 78 checks reference, not positions).
    val reference =
      KeyScreenshotReference().apply {
        this.key = this@ScreenshotKeysHighlighterTest.key
        this.screenshot = this@ScreenshotKeysHighlighterTest.screenshot
        this.positions = null
      }
    screenshot.keyScreenshotReferences.add(reference)

    val highlighter = ScreenshotKeysHighlighter(ByteArrayInputStream(storedBytes))
    val outputBytes = highlighter.highlightKey(screenshot, key.id).toByteArray()

    log.info(
      "No-positions test: stored={} bytes, re-encoded={} bytes, ratio={}x",
      storedBytes.size,
      outputBytes.size,
      String.format("%.1f", outputBytes.size.toDouble() / storedBytes.size),
    )

    assertThat(outputBytes.size)
      .withFailMessage(
        "Re-encoded image (%d bytes) exceeds the 5 MB API limit even though " +
          "nothing was highlighted. Stored image was only %d bytes (ratio: %.1fx).",
        outputBytes.size,
        storedBytes.size,
        outputBytes.size.toDouble() / storedBytes.size,
      ).isLessThan(MAX_API_SIZE_BYTES)
  }

  @Test
  fun `screenshot with highlight positions should not inflate beyond API limit`() {
    val reference =
      KeyScreenshotReference().apply {
        this.key = this@ScreenshotKeysHighlighterTest.key
        this.screenshot = this@ScreenshotKeysHighlighterTest.screenshot
        this.positions =
          mutableListOf(
            KeyInScreenshotPosition(x = 100, y = 100, width = 300, height = 80),
          )
      }
    screenshot.keyScreenshotReferences.add(reference)

    val highlighter = ScreenshotKeysHighlighter(ByteArrayInputStream(storedBytes))
    val highlightedBytes = highlighter.highlightKey(screenshot, key.id).toByteArray()

    log.info(
      "With-positions test: stored={} bytes, highlighted={} bytes, ratio={}x",
      storedBytes.size,
      highlightedBytes.size,
      String.format("%.1f", highlightedBytes.size.toDouble() / storedBytes.size),
    )

    assertThat(highlightedBytes.size)
      .withFailMessage(
        "Highlighted image (%d bytes) exceeds the 5 MB API limit. " +
          "Stored image was only %d bytes (ratio: %.1fx).",
        highlightedBytes.size,
        storedBytes.size,
        highlightedBytes.size.toDouble() / storedBytes.size,
      ).isLessThan(MAX_API_SIZE_BYTES)
  }

  /**
   * Creates a PNG byte stream with varied content (gradients, shapes, text)
   * that exercises PNG compression realistically — solid-color images compress
   * trivially and would not reproduce the bug.
   */
  private fun createRealisticScreenshot(
    width: Int,
    height: Int,
  ): ByteArrayInputStream {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()

    // Background gradient
    g.paint = GradientPaint(0f, 0f, Color(240, 240, 245), width.toFloat(), height.toFloat(), Color(200, 210, 230))
    g.fillRect(0, 0, width, height)

    // Simulate UI elements: colored rectangles, text areas, borders
    val colors =
      listOf(
        Color(255, 255, 255),
        Color(59, 130, 246),
        Color(239, 68, 68),
        Color(34, 197, 94),
        Color(168, 85, 247),
        Color(249, 115, 22),
        Color(20, 184, 166),
      )

    // Draw grid of "cards" like a real UI
    val cardWidth = 350
    val cardHeight = 200
    val padding = 20
    var colorIdx = 0
    var y = padding
    while (y + cardHeight < height) {
      var x = padding
      while (x + cardWidth < width) {
        g.color = Color.WHITE
        g.fillRect(x, y, cardWidth, cardHeight)
        g.color = Color(200, 200, 200)
        g.drawRect(x, y, cardWidth, cardHeight)
        g.color = colors[colorIdx % colors.size]
        g.fillRect(x, y, cardWidth, 40)
        g.color = Color(60, 60, 60)
        for (line in 0 until 5) {
          val lineWidth = cardWidth - 40 - (line * 20)
          if (lineWidth > 0) {
            g.fillRect(x + 15, y + 55 + line * 25, lineWidth, 12)
          }
        }
        colorIdx++
        x += cardWidth + padding
      }
      y += cardHeight + padding
    }

    g.dispose()

    val outputStream = java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(image, "png", outputStream)
    return ByteArrayInputStream(outputStream.toByteArray())
  }
}
