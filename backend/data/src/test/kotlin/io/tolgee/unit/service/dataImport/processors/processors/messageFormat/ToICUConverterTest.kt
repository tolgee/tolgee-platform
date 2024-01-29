package io.tolgee.unit.service.dataImport.processors.processors.messageFormat

import com.ibm.icu.util.ULocale
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.po.`in`.SupportedFormat
import io.tolgee.formats.po.`in`.ToICUConverter
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class ToICUConverterTest {
  private lateinit var context: FileProcessorContext
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto

  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("exmample.po", importMock)
    importFileDto =
      ImportFileDto(
        "exmample.po",
        File("src/test/resources/import/po/example.po")
          .readBytes(),
      )
    context = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun testPhpPlurals() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PHP).convertPoPlural(
        mapOf(
          0 to "Petr má jednoho psa.",
          1 to "Petr má %d psi.",
          2 to "Petr má %d psů.",
        ),
      )
    assertThat(result).isEqualTo(
      "{0, plural,\n" +
        "one {Petr má jednoho psa.}\n" +
        "few {Petr má {0, number} psi.}\n" +
        "other {Petr má {0, number} psů.}\n" +
        "}",
    )
  }

  @Test
  fun testPhpMessage() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
        .convert("hello this is string %s, this is digit %d")
    assertThat(result).isEqualTo("hello this is string {0}, this is digit {1, number}")
  }

  @Test
  fun testPhpMessageEscapes() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
        .convert("%%s %%s %%%s %%%%s")
    assertThat(result).isEqualTo("%s %s %{0} %%s")
  }

  @Test
  fun testPhpMessageWithFlags() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
        .convert("%+- 'as %+- 10s %1$'a +-010s")
    assertThat(result).isEqualTo("{0} {1} {0}")
  }

  @Test
  fun testPhpMessageMultiple() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
        .convert("%s %d %d %s")
    assertThat(result).isEqualTo("{0} {1, number} {2, number} {3}")
  }

  @Test
  fun testCMessage() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.C)
        .convert("%s %d %c %+- #0f %+- #0llf %+-hhs %0hs %jd")
    assertThat(result).isEqualTo("{0} {1, number} {2} {3, number, .000000} {4, number, .000000} {5} {6} {7, number}")
  }

  @Test
  fun testPythonMessage() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PYTHON)
        .convert("%(one)s %(two)d %(three)+- #0f %(four)+- #0lf %(five)+-hs %(six)0hs %(seven)ld")
    assertThat(
      result,
    ).isEqualTo("{one} {two, number} {three, number, .000000} {four, number, .000000} {five} {six} {seven, number}")
  }

  @Test
  fun testPhpMessageKey() {
    val result =
      ToICUConverter(ULocale("cs"), SupportedFormat.PHP)
        .convert("%3${'$'}d hello this is string %2${'$'}s, this is digit %1${'$'}d, and another digit %s")

    assertThat(result)
      .isEqualTo("{2, number} hello this is string {1}, this is digit {0, number}, and another digit {3}")
  }
}
