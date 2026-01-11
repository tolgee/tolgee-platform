package io.tolgee.unit.formats.escaping

import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.formats.escaping.IcuUnescaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class TwoWayEscapingTest {
  @Test
  fun `it works`() {
    testString("'What ' complex '' { string # ", false)
    testString("''", false)
    testString("'#'", false)
    testString("{}", false)
    testString("{aa}", false)
    testString("'{", false)
    testString("'{ }", false)
    testString("'{ }}", false)
    testString("{", false)
    testString("''", false)
    testString("Another ''' more complex ' '{ string }' with many weird } cases '", false)
    testString("Another ''' more complex ' '{ string }' with many weird } cases '}", false)
    testString("this {is} variant", false)
    testString("this '{is}' variant", false)
    testString("apostrophe ' is here", false)
    testString("hash # is here", false)
    testString("this is '' not {param} escaped", false)
    testString("this is ''' actually #' escaped", false)
    testString("should be '# }' escaped", false)
    testString("test '", false)
    testString("'<'", false)
  }

  fun testString(
    string: String,
    plural: Boolean,
  ) {
    val escaped = ForceIcuEscaper(string, plural).escaped
    val unescaped = IcuUnescaper(escaped, plural).unescaped
    unescaped.assert
      .describedAs(
        "\n\nInput:\n$string \n\nEscaped:\n$escaped \n\nUnescpaed: \n$unescaped",
      ).isEqualTo(string)
  }
}
