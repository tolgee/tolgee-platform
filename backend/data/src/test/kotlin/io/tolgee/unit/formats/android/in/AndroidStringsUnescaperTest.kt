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
    " \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 ".assertUnescaped("a a")
  }

  @Test
  fun `only opening quote is unescaped`() {
    "a a\"      ".assertUnescaped("a a      ")
  }

  @Test
  fun `doesnt keep leading space when not first`() {
    "  a".assertUnescaped(" a", isFirst = false, isLast = true)
  }

  @Test
  fun `doesnt keep trailing space when not last`() {
    "a  ".assertUnescaped("a ", isFirst = true, isLast = false)
  }

  @Test
  fun `escaped chars are unescaped`() {
    "\\\" \\' \\n \\t \\\\".assertUnescaped("\" ' \n \t \\")
  }

  @Test
  fun `unescapes all except unicode chars`() {
    "1\\% text \\’ \\u1090 \\!".assertUnescaped("1% text ’ \\u1090 !")
  }

  @Test
  fun `unescapes apos`() {
    "So funktioniert\\'s".assertUnescaped("So funktioniert's")
  }

  private fun String.assertUnescaped(
    expected: String,
    isFirst: Boolean = true,
    isLast: Boolean = true,
  ) {
    AndroidStringUnescaper(this.asSequence(), isFirst = isFirst, isLast = isLast).result.assert.isEqualTo(expected)
  }
}
