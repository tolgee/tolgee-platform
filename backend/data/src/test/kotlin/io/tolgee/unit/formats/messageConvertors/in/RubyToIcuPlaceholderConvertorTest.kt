package io.tolgee.unit.formats.messageConvertors.`in`

import io.tolgee.formats.convertMessage
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class RubyToIcuPlaceholderConvertorTest {
  @Test
  fun converts() {
    convertMessage(
      """
      Hello %{name}!
      Hey I am %s!
      Look at me %d!
      What a param %{name}!
      %<hello>0.2f
      %<hello>.2f
      %<hello>s
      %2${'$'}+-s
      %#20.8b
      """.trimIndent(),
      isInPlural = false,
      convertPlaceholders = true,
      isProjectIcuEnabled = true,
    ) {
      RubyToIcuPlaceholderConvertor()
    }.assert.isEqualTo(
      """
      Hello {name}!
      Hey I am {1}!
      Look at me {2, number}!
      What a param {name}!
      %<hello>0.2f
      {hello, number, .00}
      {hello}
      %2${'$'}+-s
      %#20.8b
      """.trimIndent(),
    )
  }
}
