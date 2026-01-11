package io.tolgee.unit.formats.messageConvertors.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.paramConvertors.out.IcuToRubyPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuToRubyParamConvertorTest {
  @Test
  fun converts() {
    "{name}".assertConvertedTo("%{name}")
    "{name, number}".assertConvertedTo("%<name>d")
    "{name, number, 0.000}".assertConvertedTo("%<name>.3f")
    "{name, number, scientific}".assertConvertedTo("%<name>e")
    "{0}".assertConvertedTo("%s")
    "{0, number}".assertConvertedTo("%d")
    "{0, number, 0.000}".assertConvertedTo("%.3f")
    "{0, number, scientific}".assertConvertedTo("%e")
    "Hello {2} I am string {1, number} with multiple {0, number, 0.000} arguments"
      .assertConvertedTo("Hello %3\$s I am string %2\$d with multiple %1\$.3f arguments")
  }

  private fun String.assertConvertedTo(expected: String) {
    MessageConvertorFactory(
      this,
      false,
      true,
    ) {
      IcuToRubyPlaceholderConvertor()
    }.create().convert().singleResult.assert.isEqualTo(expected)
  }
}
