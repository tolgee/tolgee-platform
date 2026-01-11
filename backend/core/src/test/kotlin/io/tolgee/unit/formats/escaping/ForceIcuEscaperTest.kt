package io.tolgee.unit.formats.escaping

import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class ForceIcuEscaperTest {
  @Test
  fun testHandlesParameter() {
    assertThat(ForceIcuEscaper("this {is} variant").escaped).isEqualTo("this '{'is'}' variant")
  }

  @Test
  fun testHandlesAlreadyEscapedParameter() {
    assertThat(ForceIcuEscaper("this '{is}' variant").escaped).isEqualTo("this '''{'is'}''' variant")
  }

  @Test
  fun testHandlesTextWithApostrophe() {
    assertThat(ForceIcuEscaper("apostrophe ' is here").escaped).isEqualTo("apostrophe ' is here")
  }

  @Test
  fun testEscapesHash() {
    assertThat(ForceIcuEscaper("hash # is here", escapeHash = true).escaped).isEqualTo("hash '#' is here")
  }

  @Test
  fun testHandlesDoubleQuotes() {
    assertThat(
      ForceIcuEscaper("this is '' not {param} escaped").escaped,
    ).isEqualTo("this is '''' not '{'param'}' escaped")
  }

  @Test
  fun testHandlesTripleQuotes() {
    assertThat(
      ForceIcuEscaper("this is ''' actually #' escaped", escapeHash = true).escaped,
    ).isEqualTo("this is ''''' actually '#''' escaped")
  }

  @Test
  fun testTakesHashAsEscapeCharacter() {
    assertThat(
      ForceIcuEscaper("should be '# }' escaped", escapeHash = true).escaped,
    ).isEqualTo("should be '''#' '}''' escaped")
  }

  @Test
  fun testEscapesDanglingEscapeAtTheEnd() {
    assertThat(ForceIcuEscaper("test '").escaped).isEqualTo("test ''")
  }

  @Test
  fun testDoesntTakeTagsEscapesIntoConsideration() {
    assertThat(ForceIcuEscaper("'<'").escaped).isEqualTo("'<''")
  }
}
