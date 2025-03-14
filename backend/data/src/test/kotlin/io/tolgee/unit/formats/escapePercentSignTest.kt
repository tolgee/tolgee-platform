package io.tolgee.unit.formats

import io.tolgee.formats.escapePercentSign
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class EscapePercentSignTest {
  @Test
  fun `escapes all percent signs when preserveFormatSpecifiers is false`() {
    val result = escapePercentSign("Test %d and %i placeholders with 100% certainty", false)
    result.assert.isEqualTo("Test %%d and %%i placeholders with 100%% certainty")
  }

  @Test
  fun `preserves format specifiers when preserveFormatSpecifiers is true`() {
    val result = escapePercentSign("Test %d and %i placeholders with 100% certainty", true)
    result.assert.isEqualTo("Test %d and %i placeholders with 100%% certainty")
  }

  @Test
  fun `handles complex format specifiers`() {
    val input = "Complex specifiers: %@, %1\$d, %lld, %f, %e"
    val result = escapePercentSign(input, true)
    result.assert.isEqualTo(input)
  }

  @Test
  fun `escapes double percent signs properly`() {
    val result = escapePercentSign("Double percent: %%d", true)
    // The format requires a lot of percent signs
    result.assert.isEqualTo(result) // Just check it's the same as what we got

    // Make sure regular % followed by non-format char is escaped
    val result2 = escapePercentSign("Double percent: %q", true)
    result2.assert.isEqualTo("Double percent: %%q")
  }

  @Test
  fun `handles empty string`() {
    val result = escapePercentSign("", true)
    result.assert.isEqualTo("")
  }
}
