package io.tolgee.unit

import io.tolgee.helpers.TextHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TextHelperTest {
  @Test
  fun splitOnNonEscapedDelimiter() {
    val str = "this.is.escaped\\.delimiter.aaa.once\\.more.and.multiple\\\\\\.and.\\\\\\\\.text"
    val split = TextHelper.splitOnNonEscapedDelimiter(str, '.')
    assertThat(split).isEqualTo(
      listOf("this", "is", "escaped.delimiter", "aaa", "once.more", "and", "multiple\\.and", "\\\\", "text"),
    )
  }

  @Test
  fun `replaces ICU params in standard text`() {
    val result = TextHelper.replaceIcuParams("Hello! I am standard text!")
    assertThat(result.text)
      .isEqualTo("Hello! I am standard text!")
    assertThat(result.params.size).isEqualTo(0)
    assertThat(result.isComplex).isFalse
  }

  @Test
  fun `replaces ICU params in text with params`() {
    val result =
      TextHelper.replaceIcuParams(
        "{name} Hello! " +
          "{aaa, plural, " +
          "one {What} " +
          "other {what} } " +
          "I am standard text! {anotherParam}",
      )
    assertThat(result.text)
      .isEqualTo("{xx0xx} Hello! {xx1xx} I am standard text! {xx2xx}")
    assertThat(result.isComplex).isEqualTo(true)
    assertThat(result.params.size).isEqualTo(3)
    assertThat(result.params["{xx0xx}"]).isEqualTo("{name}")
    assertThat(result.params["{xx1xx}"]).isEqualTo("{aaa, plural, one {What} other {what} }")
    assertThat(result.params["{xx2xx}"]).isEqualTo("{anotherParam}")
  }

  @Test
  fun `replaces ICU params with escaping`() {
    val result =
      TextHelper.replaceIcuParams(
        "'{name} Hello! " +
          "{aaa, plural, " +
          "one {What} " +
          "other {what} } " +
          "I am standard text! {anotherParam}" +
          "That's cool! That''s cool as well!",
      )
    assertThat(result.text)
      .isEqualTo("{name} Hello! {xx0xx} I am standard text! {xx1xx}That's cool! That's cool as well!")
    assertThat(result.isComplex).isEqualTo(true)
    assertThat(result.params.size).isEqualTo(2)
    assertThat(result.params["{xx0xx}"]).isEqualTo("{aaa, plural, one {What} other {what} }")
    assertThat(result.params["{xx1xx}"]).isEqualTo("{anotherParam}")
  }
}
