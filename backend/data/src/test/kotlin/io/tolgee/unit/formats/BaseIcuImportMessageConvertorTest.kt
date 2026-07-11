package io.tolgee.unit.formats

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.NoOpFromIcuPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class BaseIcuImportMessageConvertorTest {
  @Test
  fun `converts plural`() {
    val forms =
      BaseIcuMessageConvertor(
        "Hello! I have {count, plural, other {# dogs} one {# dog} many {# dogs}}. " +
          "Did you know? Here is a number {num, number}",
        { NoOpFromIcuPlaceholderConvertor() },
      ).convert().formsResult!!
    forms["one"].assert.isEqualTo("Hello! I have # dog. Did you know? Here is a number {num, number}")
    forms["many"].assert.isEqualTo("Hello! I have # dogs. Did you know? Here is a number {num, number}")
    forms["other"].assert.isEqualTo("Hello! I have # dogs. Did you know? Here is a number {num, number}")
    forms.keys.size.assert
      .isEqualTo(3)
  }

  @Test
  fun `works with forced isPlural = true`() {
    val forms =
      BaseIcuMessageConvertor(
        "Hello!",
        { NoOpFromIcuPlaceholderConvertor() },
        forceIsPlural = true,
      ).convert().formsResult!!
    forms["other"].assert.isEqualTo("Hello!")
    forms.keys.size.assert
      .isEqualTo(1)
  }

  @Test
  fun `works with forced isPlural = false`() {
    BaseIcuMessageConvertor(
      "{num, plural, one {# dog} other {# dogs}}",
      { NoOpFromIcuPlaceholderConvertor() },
      forceIsPlural = false,
    ).convert().singleResult.assert.isEqualTo("{num, plural, one {# dog} other {# dogs}}")
  }
}
