package io.tolgee.unit.formatConversions

import io.tolgee.formats.paramConvertors.`in`.CToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToCPlaceholderConvertor
import io.tolgee.formats.po.`in`.PoToIcuMessageConvertor
import io.tolgee.formats.po.out.IcuToPoMessageConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class CPoConversionTest {
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
    convertToIcu("Hello %.51f")
      .assert
      .isEqualTo("Hello %.51f")
  }

  private fun testString(string: String) {
    val icuString = convertToIcu(string)
    val cString =
      IcuToPoMessageConvertor(
        icuString!!,
        forceIsPlural = false,
        placeholderConvertor = IcuToCPlaceholderConvertor(),
      ).convert().singleResult
    cString.assert
      .describedAs("Input:\n${string}\nICU:\n$icuString\nC String:\n$cString")
      .isEqualTo(string)
  }

  private fun convertToIcu(string: String) =
    PoToIcuMessageConvertor { CToIcuPlaceholderConvertor() }.convert(string, "en").message
}
