package io.tolgee.unit.formatConversions

import com.ibm.icu.util.ULocale
import io.tolgee.formats.po.SupportedFormat
import io.tolgee.formats.po.`in`.PoToICUConverter
import io.tolgee.formats.po.out.python.ToPythonPoMessageConverter
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PythonPoConversionTest {
  @Test
  fun `it transforms`() {
    testString("Hello %(a)s")
    testString("Hello %(a)d")
    testString("Hello %(a).2f")
    testString("Hello %(a)f")
    testString("Hello %(a)e")
    testString("Hello %(a)e, hello %(v)s")
    testString("Hello %(a).50f")
    testString("Hello %(a).50f")
  }

  @Test
  fun `it limits precision`() {
    val precisionString = (1..50).joinToString("") { "0" }
    convertToIcu("Hello %(a).51f").assert.isEqualTo("Hello {a, number, .$precisionString}")
  }

  private fun testString(string: String) {
    val icuString = convertToIcu(string)
    val pythonString = ToPythonPoMessageConverter(icuString).convert().singleResult
    pythonString.assert
      .describedAs("Input:\n${string}\nICU:\n$icuString\nPython String:\n$pythonString")
      .isEqualTo(string)
  }

  private fun convertToIcu(string: String) = PoToICUConverter(ULocale.ENGLISH, SupportedFormat.PYTHON).convert(string)
}
