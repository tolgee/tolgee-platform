package io.tolgee.unit.formats.escaping

import io.tolgee.formats.escaping.PluralFormIcuEscaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PluralFormIcuEscaperTest {
  @Test
  fun `it escapes`() {
    PluralFormIcuEscaper("{hello}, my friend!").escaped.assert.isEqualTo("'{hello}', my friend!")
  }

  @Test
  fun `it works for weird case`() {
    PluralFormIcuEscaper(
      "'What ' complex '' { string # ",
      false,
    ).escaped.assert.isEqualTo("'What ' complex '' '{' string # ")
  }

  @Test
  fun `it it escapes escaped`() {
    PluralFormIcuEscaper(
      "'{",
      false,
    ).escaped.assert.isEqualTo("'{'")
  }

  @Test
  fun `it escapes apostrophes`() {
    PluralFormIcuEscaper(
      "we are not entering escaped section: '' " +
        "so it doesn't ' have to be doubled. " +
        "This sequence: { should be immediately closed",
    ).escaped.assert
      .isEqualTo(
        "we are not entering escaped section: '' " +
          "so it doesn't ' have to be doubled. " +
          "This sequence: '{' should be immediately closed",
      )
  }

  @Test
  fun `it escapes plurals`() {
    PluralFormIcuEscaper(
      "What a # plural form",
      escapeHash = true,
    ).escaped.assert.isEqualTo("What a '#' plural form")
  }
}
