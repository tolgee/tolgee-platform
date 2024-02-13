package io.tolgee.unit.formatConversions

import com.ibm.icu.util.ULocale
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.formats.po.`in`.PoToICUConverter
import io.tolgee.formats.po.out.php.ToPhpPoMessageConverter
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
    val phpString = ToPhpPoMessageConverter(icuString, forceIsPlural = false).convert().singleResult
    phpString.assert
      .describedAs("Input:\n${string}\nICU:\n$icuString\nPhpString:\n$phpString")
      .isEqualTo(string)
  }

  private fun convertToIcu(string: String) = PoToICUConverter(ULocale.ENGLISH, SupportedFormat.PHP).convert(string)
}
