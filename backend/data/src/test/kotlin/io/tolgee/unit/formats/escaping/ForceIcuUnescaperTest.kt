package io.tolgee.unit.formats.escaping

import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.formats.escaping.ForceIcuUnescaper
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForceIcuUnescaperTest {
  @Test
  fun testHandlesParameter() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("this {is} variant").escaped).unescaped)
      .isEqualTo("this {is} variant")
  }

  @Test
  fun testHandlesAlreadyEscapedParameter() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("this '{is}' variant").escaped).unescaped)
      .isEqualTo("this '{is}' variant")
  }

  @Test
  fun testHandlesTextWithApostrophe() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("apostrophe ' is here").escaped).unescaped)
      .isEqualTo("apostrophe ' is here")
  }

  @Test
  fun testEscapesHash() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("hash # is here").escaped).unescaped)
      .isEqualTo("hash # is here")
  }

  @Test
  fun testHandlesDoubleQuotes() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("this is '' not {param} escaped").escaped).unescaped)
      .isEqualTo("this is '' not {param} escaped")
  }

  @Test
  fun testHandlesTripleQuotes() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("this is ''' actually #' escaped").escaped).unescaped)
      .isEqualTo("this is ''' actually #' escaped")
  }

  @Test
  fun testTakesHashAsEscapeCharacter() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("should be '# }' escaped").escaped).unescaped)
      .isEqualTo("should be '# }' escaped")
  }

  @Test
  fun testEscapesDanglingEscapeAtTheEnd() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("test '").escaped).unescaped)
      .isEqualTo("test '")
  }

  @Test
  fun testDoesntTakeTagsEscapesIntoConsideration() {
    assertThat(ForceIcuUnescaper(ForceIcuEscaper("'<'").escaped).unescaped)
      .isEqualTo("'<'")
  }
}
