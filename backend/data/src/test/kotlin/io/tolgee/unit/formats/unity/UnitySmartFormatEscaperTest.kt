package io.tolgee.unit.formats.unity

import io.tolgee.formats.unity.UnitySmartFormatEscaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class UnitySmartFormatEscaperTest {
  @Test
  fun `escapes literal braces and backslashes`() {
    UnitySmartFormatEscaper.escape("a {b} c").assert.isEqualTo("a \\{b\\} c")
    UnitySmartFormatEscaper.escape("back\\slash").assert.isEqualTo("back\\\\slash")
  }

  @Test
  fun `escape and unescape are inverse`() {
    listOf("plain", "a {b} c", "back\\slash", "{}", "mix \\{ }\\ end")
      .forEach { original ->
        UnitySmartFormatEscaper.unescape(UnitySmartFormatEscaper.escape(original)).assert.isEqualTo(original)
      }
  }
}
