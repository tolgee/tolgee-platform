package io.tolgee.unit.formats.compose.`in`

import io.tolgee.formats.compose.`in`.ComposeStringUnescaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class ComposeStringsUnescaperTest {
  @Test
  fun `keeps raw text unchanged`() {
    "\n\t\u0020\u2008\u2003\"".assertUnescaped("\n\t\u0020\u2008\u2003\"")
  }

  @Test
  fun `unquoted spaces are kept`() {
    " \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 ".assertUnescaped(
      " \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 a \n\t\u0020\u2008\u2003 ",
    )
  }

  @Test
  fun `quote does not affect text`() {
    "a a\"      ".assertUnescaped("a a\"      ")
  }

  @Test
  fun `keeps leading space`() {
    "  a".assertUnescaped("  a")
  }

  @Test
  fun `keeps trailing space`() {
    "a  ".assertUnescaped("a  ")
  }

  @Test
  fun `escaped chars are unescaped`() {
    "\\n \\t \\\\".assertUnescaped("\n \t \\")
  }

  @Test
  fun `chars that don't need escaping are kept unchanged`() {
    "\\\" \\' \" '".assertUnescaped("\\\" \\' \" '")
  }

  @Test
  fun `keeps apos`() {
    "So funktioniert's".assertUnescaped("So funktioniert's")
  }

  private fun String.assertUnescaped(expected: String) {
    ComposeStringUnescaper(this.asSequence()).result.assert.isEqualTo(expected)
  }
}
