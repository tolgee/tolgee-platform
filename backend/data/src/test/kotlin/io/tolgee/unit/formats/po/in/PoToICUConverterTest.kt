package io.tolgee.unit.formats.po.`in`

import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PythonToIcuPlaceholderConvertor
import io.tolgee.formats.po.`in`.PoToIcuMessageConvertor
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
      getPhpConvertor()
        .convert(
          rawData =
            mapOf(
              0 to "Petr má jednoho psa.",
              1 to "Petr má %d psi.",
              2 to "Petr má %d psů.",
            ),
          languageTag = "cs",
          convertPlaceholders = true,
        ).message
    assertThat(result).isEqualTo(
      "{0, plural,\n" +
        "one {Petr má jednoho psa.}\n" +
        "few {Petr má # psi.}\n" +
        "other {Petr má # psů.}\n" +
        "}",
    )
  }

  @Test
  fun `php plurals with hashtags works`() {
    val result =
      getPhpConvertor()
        .convert(
          rawData =
            mapOf(
              0 to "Petr má jeden znak #.",
              1 to "Petr má %d znaky #.",
              2 to "Petr má %d znaků #.",
            ),
          languageTag = "cs",
          convertPlaceholders = true,
        ).message
    assertThat(result).isEqualTo(
      "{0, plural,\n" +
        "one {Petr má jeden znak '#'.}\n" +
        "few {Petr má # znaky '#'.}\n" +
        "other {Petr má # znaků '#'.}\n" +
        "}",
    )
  }

  @Test
  fun `php plurals with hashtags and disabled placeholders works`() {
    val result =
      getPhpConvertor()
        .convert(
          rawData =
            mapOf(
              0 to "Petr má jeden znak #.",
              1 to "Petr má %d znaky #.",
              2 to "Petr má %d znaků #.",
            ),
          languageTag = "cs",
          convertPlaceholders = false,
        ).message
    assertThat(result).isEqualTo(
      "{value, plural,\n" +
        "one {Petr má jeden znak '#'.}\n" +
        "few {Petr má %d znaky '#'.}\n" +
        "other {Petr má %d znaků '#'.}\n" +
        "}",
    )
  }

  private fun getPhpConvertor() = PoToIcuMessageConvertor { PhpToIcuPlaceholderConvertor() }

  @Test
  fun testPhpMessage() {
    val result =
      getPhpConvertor().convert("hello this is string %s, this is digit %d", "en").message
    assertThat(result).isEqualTo("hello this is string {0}, this is digit {1, number}")
  }

  @Test
  fun `php message with hashtag works`() {
    val result =
      getPhpConvertor().convert("hello this is hashtag # and it should not be escaped", "en").message
    assertThat(result).isEqualTo("hello this is hashtag # and it should not be escaped")
  }

  @Test
  fun `php message with hashtag and disabled placeholders works`() {
    val result =
      getPhpConvertor()
        .convert(
          "hello this is hashtag # and it should not be escaped",
          "en",
          convertPlaceholders = false,
        ).message
    assertThat(result).isEqualTo("hello this is hashtag # and it should not be escaped")
  }

  @Test
  fun testPhpMessageEscapes() {
    val result =
      getPhpConvertor().convert("%%s %%s %%%s %%%%s", "cs").message
    assertThat(result).isEqualTo("%s %s %{0} %%s")
  }

  @Test
  fun testPhpMessageWithFlags() {
    val result =
      getPhpConvertor().convert("%+- 'as %+- 10s %1$'a +-010s", "cs").message
    assertThat(result).isEqualTo("%+- 'as %+- 10s %1$'a +-010s")
  }

  @Test
  fun testPhpMessageMultiple() {
    val result =
      getPhpConvertor().convert("%s %d %d %s", "cs").message
    assertThat(result).isEqualTo("{0} {1, number} {2, number} {3}")
  }

  @Test
  fun testPythonMessage() {
    val result =
      getPythonConvertor()
        .convert(
          "%(one)s %(two)d %(three)+- #0f %(four)+- #0lf %(five)+-hs %(six)0hs %(seven)ld {hey}",
          "cs",
          true,
        ).message
    assertThat(
      result,
    ).isEqualTo(
      "{one} {two, number} %(three)+- #0f %(four)+- #0lf %(five)+-hs %(six)0hs %(seven)ld '{'hey'}'",
    )
  }

  private fun getPythonConvertor() = PoToIcuMessageConvertor { PythonToIcuPlaceholderConvertor() }

  @Test
  fun testPhpMessageKey() {
    val result =
      getPhpConvertor()
        .convert(
          "%3${'$'}d hello this is string %2${'$'}s, this is digit %1${'$'}d, and another digit %s",
          "cs",
          true,
        ).message

    assertThat(result)
      .isEqualTo("{2, number} hello this is string {1}, this is digit {0, number}, and another digit {3}")
  }
}
