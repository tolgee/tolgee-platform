package io.tolgee.unit.formats.apple.out

import io.tolgee.formats.PossiblePluralConversionResult
import io.tolgee.formats.apple.out.IcuToAppleMessageConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuToAppleImportMessageConvertorTest {
  @Test
  fun `converts # to li when plural`() {
    val result = "{param, plural, other {# dogs}}".getConversionResult(forceIsPlural = true)
    result.formsResult!!["other"]!!.assert.isEqualTo("%lld dogs")
  }

  private fun String.getConversionResult(forceIsPlural: Boolean = false): PossiblePluralConversionResult {
    val result = IcuToAppleMessageConvertor(this, forceIsPlural).convert()
    return result
  }

  @Test
  fun `converts param to @`() {
    "hello {name}".assertSingleConverted("hello %@")
  }

  @Test
  fun `converts number to lld`() {
    "hello {name, number}".assertSingleConverted("hello %lld")
  }

  @Test
  fun `converts float to float`() {
    "hello {name, number, 0.00}".assertSingleConverted("hello %.2f")
  }

  @Test
  fun `numbers correctly`() {
    "hello {2, number, 0.00} {1, number}".assertSingleConverted("hello %3$.2f %2${'$'}lld")
  }

  @Test
  fun `numbers correctly in plurals`() {
    val forms =
      "{number, plural, other {# {2} {1}} one {{1} # {2}}}".getConversionResult(forceIsPlural = true).formsResult!!
    forms["other"].assert.isEqualTo("%lld %3${'$'}@ %2${'$'}@")
    forms["one"].assert.isEqualTo("%2${'$'}@ %lld %3${'$'}@")
  }

  private fun String.assertSingleConverted(expected: String) {
    val result = IcuToAppleMessageConvertor(message = this, forceIsPlural = false).convert()
    result.singleResult.assert.isEqualTo(expected)
  }
}
