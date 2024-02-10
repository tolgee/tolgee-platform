package io.tolgee.unit.formats

import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.NoOpFromIcuParamConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class BaseIcuMessageConvertorTest {
  @Test
  fun `converts plural`() {
    val forms =
      BaseIcuMessageConvertor(
        "Hello! I have {count, plural, other {# dogs} one {# dog} many {# dogs}}. " +
          "Did you know? Here is a number {num, number}",
        NoOpFromIcuParamConvertor(),
      ).convert().formsResult!!
    forms["one"].assert.isEqualTo("Hello! I have # dog. Did you know? Here is a number {num,number}")
    forms["many"].assert.isEqualTo("Hello! I have # dogs. Did you know? Here is a number {num,number}")
    forms["other"].assert.isEqualTo("Hello! I have # dogs. Did you know? Here is a number {num,number}")
    forms.keys.size.assert.isEqualTo(3)
  }
}
