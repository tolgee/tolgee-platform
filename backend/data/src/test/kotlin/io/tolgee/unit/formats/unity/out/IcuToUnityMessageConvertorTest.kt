package io.tolgee.unit.formats.unity.out

import io.tolgee.formats.unity.out.IcuToUnityMessageConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuToUnityMessageConvertorTest {
  @Test
  fun `converts a named placeholder and reports it as smart`() {
    val result = convert("Hi {name}")
    result.isPlural().assert.isEqualTo(false)
    result.firstArgName.assert.isEqualTo("name")
    result.singleResult.assert.isEqualTo("Hi {name}")
  }

  @Test
  fun `treats a pure literal as non-smart and escapes literal braces`() {
    val result = convert("use '{'brackets'}'")
    result.isPlural().assert.isEqualTo(false)
    result.firstArgName.assert.isEqualTo(null)
    result.singleResult.assert.isEqualTo("use \\{brackets\\}")
  }

  @Test
  fun `converts plural forms and the hash number`() {
    val result = convert("{count, plural, one {# apple} other {# apples}}", forceIsPlural = true)
    result.isPlural().assert.isEqualTo(true)
    result.formsResult!!["one"].assert.isEqualTo("{} apple")
    result.formsResult!!["other"].assert.isEqualTo("{} apples")
  }

  private fun convert(
    message: String,
    forceIsPlural: Boolean = false,
  ) = IcuToUnityMessageConvertor(message, forceIsPlural, isProjectIcuPlaceholdersEnabled = true).convert()
}
