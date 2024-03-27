package io.tolgee.unit.formats.messageConvertors.`in`

import io.tolgee.formats.convertMessage
import io.tolgee.formats.paramConvertors.`in`.RubyToIcuPlaceholderConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class RubyToIcuPlaceholderConvertorTest {
  @Test
  fun converts() {
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
    """.trimIndent()
      .assertConverted(
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

  @Test
  fun `only first plural param is converted`() {
    val converted = convertInPlural("I have %{count} dogs and I am %{name}.")
    converted.message.assert.isEqualTo(
      "I have # dogs and I am {name}.",
    )
    converted.pluralArgName.assert.isEqualTo("count")
  }

  fun String.assertConverted(expected: String) {
    convertMessage(
      this,
      isInPlural = false,
      convertPlaceholders = true,
      isProjectIcuEnabled = true,
    ) {
      RubyToIcuPlaceholderConvertor()
    }.message.assert.isEqualTo(expected)
  }

  private fun convertInPlural(string: String) =
    convertMessage(
      string,
      isInPlural = true,
      convertPlaceholders = true,
      isProjectIcuEnabled = true,
    ) {
      RubyToIcuPlaceholderConvertor()
    }
}
