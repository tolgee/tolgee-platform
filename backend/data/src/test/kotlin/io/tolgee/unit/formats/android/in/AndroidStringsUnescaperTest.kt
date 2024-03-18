package io.tolgee.unit.formats.android.`in`

import io.tolgee.formats.android.`in`.AndroidStringUnescaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class AndroidStringsUnescaperTest {
  @Test
  fun `quoted string is correctly unescaped`() {
    "\" \n\t\u0020\u2008\u2003\"".assertUnescaped(" \n\t\u0020\u2008\u2003")
  }

  @Test
  fun `unquoted spaces are correctly trimmed`() {
    " \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 ".assertUnescaped(" a ")
  }

  @Test
  fun `only opening quote is unescaped`() {
    "a a\"      ".assertUnescaped("a a      ")
  }

  @Test
  fun `escaped chars are unescaped`() {
    "\\\" \\' \\n \\t \\\\".assertUnescaped("\" ' \n \t \\")
  }

  private fun String.assertUnescaped(expected: String) {
    AndroidStringUnescaper(this).unescaped.assert.isEqualTo(expected)
  }
}
