package io.tolgee.unit.formats.po.`in`

import io.tolgee.formats.po.`in`.messageConverters.PoCToIcuMessageConverter
import io.tolgee.formats.po.`in`.messageConverters.PoPhpToIcuMessageConverter
import io.tolgee.formats.po.`in`.messageConverters.PoPythonToIcuMessageConverter
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PoToICUConverterTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.po", "src/test/resources/import/po/example.po")
  }

  @Test
  fun testPhpPlurals() {
    val result =
      PoPhpToIcuMessageConverter().convert(
        mapOf(
          0 to "Petr má jednoho psa.",
          1 to "Petr má %d psi.",
          2 to "Petr má %d psů.",
        ),
        "cs",
      ).message
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
      PoPhpToIcuMessageConverter().convert("hello this is string %s, this is digit %d", "en").message
    assertThat(result).isEqualTo("hello this is string {0}, this is digit {1, number}")
  }

  @Test
  fun testPhpMessageEscapes() {
    val result =
      PoPhpToIcuMessageConverter().convert("%%s %%s %%%s %%%%s", "cs").message
    assertThat(result).isEqualTo("%s %s %{0} %%s")
  }

  @Test
  fun testPhpMessageWithFlags() {
    val result =
      PoPhpToIcuMessageConverter().convert("%+- 'as %+- 10s %1$'a +-010s", "cs").message
    assertThat(result).isEqualTo("%+- 'as %+- 10s %1$'a +-010s")
  }

  @Test
  fun testPhpMessageMultiple() {
    val result =
      PoPhpToIcuMessageConverter().convert("%s %d %d %s", "cs").message
    assertThat(result).isEqualTo("{0} {1, number} {2, number} {3}")
  }

  @Test
  fun testCMessage() {
    val result =
      PoCToIcuMessageConverter().convert("%s %d %c %+- #0f %+- #0llf %+-hhs %0hs {hey} %jd", "cs").message
    assertThat(
      result,
    ).isEqualTo("{0} {1, number} %c %+- #0f %+- #0llf %+-hhs %0hs '{hey}' %jd")
  }

  @Test
  fun testPythonMessage() {
    val result =
      PoPythonToIcuMessageConverter().convert(
        "%(one)s %(two)d %(three)+- #0f %(four)+- #0lf %(five)+-hs %(six)0hs %(seven)ld {hey}",
        "cs",
      ).message
    assertThat(
      result,
    ).isEqualTo(
      "{one} {two, number} %(three)+- #0f %(four)+- #0lf %(five)+-hs %(six)0hs %(seven)ld '{hey}'",
    )
  }

  @Test
  fun testPhpMessageKey() {
    val result =
      PoPhpToIcuMessageConverter().convert(
        "%3${'$'}d hello this is string %2${'$'}s, this is digit %1${'$'}d, and another digit %s",
        "cs",
      ).message

    assertThat(result)
      .isEqualTo("{2, number} hello this is string {1}, this is digit {0, number}, and another digit {3}")
  }
}
