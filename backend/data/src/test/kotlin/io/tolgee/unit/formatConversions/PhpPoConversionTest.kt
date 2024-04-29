package io.tolgee.unit.formatConversions

import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToPhpPlaceholderConvertor
import io.tolgee.formats.po.`in`.PoToIcuMessageConvertor
import io.tolgee.formats.po.out.IcuToPoMessageConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PhpPoConversionTest {
  @Test
  fun `it transforms`() {
    testString("Hello %s")
    testString("Hello %d")
    testString("Hello %.2f")
    testString("Hello %f")
    testString("Hello %e")
    testString("Hello %2\$e, hello %1\$s")
    testString("Hello %.50f")
    testString("Hello %.50f")
  }

  @Test
  fun `doesn't limit precision`() {
    convertToIcu("Hello %.51f").assert.isEqualTo("Hello %.51f")
  }

  private fun testString(string: String) {
    val icuString = convertToIcu(string)
    val phpString =
      IcuToPoMessageConvertor(
        icuString!!,
        forceIsPlural = false,
        placeholderConvertor = IcuToPhpPlaceholderConvertor(),
      ).convert().singleResult
    phpString.assert
      .describedAs("Input:\n${string}\nICU:\n$icuString\nPhpString:\n$phpString")
      .isEqualTo(string)
  }

  private fun convertToIcu(string: String) =
    PoToIcuMessageConvertor { PhpToIcuPlaceholderConvertor() }.convert(string, "en").message
}
