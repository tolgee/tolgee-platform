package io.tolgee.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class XmlSanitizationTest {
  @Test
  fun `strips every illegal C0 control character except tab, LF, and CR`() {
    val invalidControls = (0x00..0x1F).filter { it != 0x09 && it != 0x0A && it != 0x0D }
    val poisoned = invalidControls.joinToString(separator = "") { "x${it.toChar()}" }

    val cleaned = sanitizeXmlText(poisoned)

    assertThat(cleaned).isEqualTo("x".repeat(invalidControls.size))
  }

  @Test
  fun `strips lone UTF-16 surrogates while keeping valid surrogate pairs intact`() {
    val cleaned = sanitizeXmlText("a\uD800b😀c\uDFFFd")

    assertThat(cleaned).isEqualTo("ab😀cd")
  }

  @Test
  fun `strips BMP noncharacters U+FFFE and U+FFFF`() {
    val cleaned = sanitizeXmlText("a${Char(0xFFFE)}b${Char(0xFFFF)}c")

    assertThat(cleaned).isEqualTo("abc")
  }

  @Test
  fun `preserves legal whitespace and supplementary plane codepoints`() {
    val input = "line1\nline2\tcol\rend 😀"

    assertThat(sanitizeXmlText(input)).isEqualTo(input)
  }

  @Test
  fun `returns empty string unchanged`() {
    assertThat(sanitizeXmlText("")).isEqualTo("")
  }
}
