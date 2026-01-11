package io.tolgee.unit.formats.messageConvertors.`in`

import io.tolgee.formats.convertMessage
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class JavaToIcuPlaceholderConvertorTest {
  @Test
  fun `percent sign and placeholder works`() {
    "%% %s %% %s".assertConverted("% {0} % {1}")
  }

  private fun String.assertConverted(expected: String) {
    convertMessage(
      this,
      isInPlural = false,
      convertPlaceholders = true,
      isProjectIcuEnabled = true,
    ) {
      RubyToIcuPlaceholderConvertor()
    }.message.assert.isEqualTo(expected)
  }
}
