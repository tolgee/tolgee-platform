package io.tolgee.unit.util

import io.tolgee.testing.assert
import io.tolgee.util.formatPathsForLog
import org.junit.jupiter.api.Test

class FormatPathsForLogTest {
  @Test
  fun `scrubs characters that would forge log lines`() {
    formatPathsForLog(listOf("en\r\nWARN faked\tline.strings"))
      .assert
      .isEqualTo("en__WARN faked_line.strings")
  }

  @Test
  fun `caps each path rather than the joined result`() {
    val formatted = formatPathsForLog(List(3) { "a".repeat(500) })
    formatted.assert.hasSize(3 * 200 + 2 * ", ".length)
  }

  @Test
  fun `lists at most three paths and counts the rest`() {
    formatPathsForLog(listOf("a", "b", "c", "d", "e"))
      .assert
      .isEqualTo("a, b, c and 2 more")
  }

  @Test
  fun `lists every path when there are no more than three`() {
    formatPathsForLog(listOf("a", "b")).assert.isEqualTo("a, b")
  }
}
