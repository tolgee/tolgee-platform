package io.tolgee.unit.service.dataImport.processors.messageFormat

import com.ibm.icu.util.ULocale
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.service.dataImport.processors.messageFormat.SupportedFormat
import io.tolgee.service.dataImport.processors.messageFormat.ToICUConverter
import org.testng.annotations.Test

class ToICUConverterTest {
    @Test
    fun testPhpPlurals() {
        val result = ToICUConverter(ULocale("cs"), SupportedFormat.PHP).convertPoPlural(
                mapOf(
                        0 to "Petr má jednoho psa.",
                        1 to "Petr má %s psi.",
                        2 to "Petr má %d psů."
                )
        )
        assertThat(result).isEqualTo("{0, plural,\n" +
                "one {Petr má jednoho psa.}\n" +
                "few {Petr má {0} psi.}\n" +
                "other {Petr má {0} psů.}\n" +
                "}")
    }

    @Test
    fun testPhpMessage() {
        val result = ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
                .convert("hello this is string %s, this is digit %d")
        assertThat(result).isEqualTo("hello this is string {0}, this is digit {1, number}")
    }

    @Test
    fun testPhpMessageMultiple() {
        val result = ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
                .convert("%s %d %d %s")
        assertThat(result).isEqualTo("{0} {1, number} {2, number} {3}")
    }

    @Test
    fun testPhpMessageKey() {
        val result = ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
                .convert("%3${'$'}d hello this is string %2${'$'}s, this is digit %1${'$'}d, and another digit %s")

        assertThat(result)
                .isEqualTo("{2, number} hello this is string {1}, this is digit {0, number}, and another digit {2}")
    }
}
