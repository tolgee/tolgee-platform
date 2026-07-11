package io.tolgee.unit.formats.messageConvertors.out

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.paramConvertors.out.IcuToJavaPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuToJavaPlaceholderConvertorTest {
  @Test
  fun `when same placeholder is used multiple times`() {
    "{0} {0} {0}".assertConvertedTo("%1\$s %1\$s %1\$s")
  }

  private fun String.assertConvertedTo(expected: String) {
    MessageConvertorFactory(
      this,
      false,
      true,
    ) {
      IcuToJavaPlaceholderConvertor()
    }.create().convert().singleResult.assert.isEqualTo(expected)
  }
}
