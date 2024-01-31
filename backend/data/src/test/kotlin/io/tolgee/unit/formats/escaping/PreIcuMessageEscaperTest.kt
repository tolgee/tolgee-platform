package io.tolgee.unit.formats.escaping

import io.tolgee.formats.PreIcuMessageEscaper
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PreIcuMessageEscaperTest {
  @Test
  fun `it escapes`() {
    PreIcuMessageEscaper("{hello}, my friend!").escaped.assert.isEqualTo("'{hello}', my friend!")
  }

  @Test
  fun `it works for weird case`() {
    PreIcuMessageEscaper(
      "'What ' complex '' { string # ",
      false,
    ).escaped.assert.isEqualTo("'What ' complex '''' '{' string # ")
  }

  @Test
  fun `it it escapes escaped`() {
    PreIcuMessageEscaper(
      "'{",
      false,
    ).escaped.assert.isEqualTo("'''{'")
  }

  @Test
  fun `it escapes apostrophes`() {
    PreIcuMessageEscaper(
      "we are not entering escaped section: '' " +
        "so it doesn't ' have to be doubled. " +
        "This sequence: { should be immediately closed",
    )
      .escaped.assert.isEqualTo(
        "we are not entering escaped section: '''' " +
          "so it doesn't ' have to be doubled. " +
          "This sequence: '{' should be immediately closed",
      )
  }

  @Test
  fun `it escapes plurals`() {
    PreIcuMessageEscaper(
      "What a # plural form",
      isPlural = true,
    ).escaped.assert.isEqualTo("What a '#' plural form")
  }
}
