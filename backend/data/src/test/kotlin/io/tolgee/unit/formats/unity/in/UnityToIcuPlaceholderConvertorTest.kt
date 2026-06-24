package io.tolgee.unit.formats.unity.`in`

import io.tolgee.formats.convertMessage
import io.tolgee.formats.paramConvertors.`in`.UnityToIcuPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class UnityToIcuPlaceholderConvertorTest {
  @Test
  fun `keeps a simple placeholder`() {
    convert("Hi {name}").assert.isEqualTo("Hi {name}")
  }

  @Test
  fun `converts the empty placeholder to a hash inside plural`() {
    convert("{} apples", isInPlural = true).assert.isEqualTo("# apples")
  }

  @Test
  fun `unescapes literal braces, pipe and backslash`() {
    convert("a \\{b\\} \\| c").assert.isEqualTo("a '{'b'}' | c")
  }

  @Test
  fun `preserves a formatter-suffixed placeholder as a literal`() {
    // {0:N2} is not bidirectionally supported -> kept verbatim (ICU-escaped)
    convert("price {0:N2}").assert.isEqualTo("price '{'0:N2'}'")
  }

  private fun convert(
    message: String,
    isInPlural: Boolean = false,
  ): String? =
    convertMessage(
      message = message,
      isInPlural = isInPlural,
      convertPlaceholders = true,
      isProjectIcuEnabled = true,
      escapeUnmatched = true,
      convertorFactory = { UnityToIcuPlaceholderConvertor() },
    ).message
}
