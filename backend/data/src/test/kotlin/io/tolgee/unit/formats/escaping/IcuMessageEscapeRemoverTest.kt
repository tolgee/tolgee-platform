package io.tolgee.unit.formats.escaping

import io.tolgee.formats.IcuMessageEscapeRemover
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuMessageEscapeRemoverTest {
  @Test
  fun `it escapes`() {
    IcuMessageEscapeRemover("'{hello}', my friend!").escapeRemoved.assert.isEqualTo("{hello}, my friend!")
  }

  @Test
  fun `it escapes apostrophes`() {
    IcuMessageEscapeRemover(
      "we are not entering escaped section: '''' " +
        "so it doesn't ' have to be doubled. " +
        "This sequence: '{' should be immediately closed",
    ).escapeRemoved.assert.isEqualTo(
      "we are not entering escaped section: '' " +
        "so it doesn't ' have to be doubled. " +
        "This sequence: { should be immediately closed",
    )
  }

  @Test
  fun `it works for weird case`() {
    IcuMessageEscapeRemover(
      "'What ' complex '''' '{' string # ",
      false,
    ).escapeRemoved.assert.isEqualTo("'What ' complex '' { string # ")
  }

  @Test
  fun `removes the escape char on end of string`() {
    val escaped = "Another ''''' more complex ' '''{ string }''' with many weird '} cases ''''}'"
    IcuMessageEscapeRemover(escaped, false)
      .escapeRemoved.assert.isEqualTo("Another ''' more complex ' '{ string }' with many weird } cases '}")
  }

  @Test
  fun `it it escapes escaped`() {
    IcuMessageEscapeRemover(
      "'''{'",
      false,
    ).escapeRemoved.assert.isEqualTo("'{")
  }

  @Test
  fun `it escapes plurals`() {
    IcuMessageEscapeRemover(
      "What a '#' plural form",
      isPlural = true,
    ).escapeRemoved.assert.isEqualTo("What a # plural form")
  }
}
