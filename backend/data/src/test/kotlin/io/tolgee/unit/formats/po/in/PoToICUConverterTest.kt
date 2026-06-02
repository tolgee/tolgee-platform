package io.tolgee.unit.formats.po.`in`

import io.tolgee.formats.MessageConvertorFactory
import io.tolgee.formats.paramConvertors.`in`.PhpToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PythonBraceToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.`in`.PythonToIcuPlaceholderConvertor
import io.tolgee.formats.paramConvertors.out.IcuToPythonBracePlaceholderConvertor
import io.tolgee.formats.po.`in`.PoToIcuMessageConvertor
import io.tolgee.formats.po.out.IcuToPoMessageConvertor
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
  fun testPythonBraceMessage() {
    val result =
      getPythonBraceConvertor()
        .convert("{one} {two:d} {three:.2f} {four!r} {five:>10}", "cs", true)
        .message
    assertThat(result).isEqualTo(
      "{one} {two, number} {three, number, .00} '{'four!r'}' '{'five:>10'}'",
    )
  }

  @Test
  fun testPythonBraceNumberSpecs() {
    val result =
      getPythonBraceConvertor()
        .convert("{a:f} {b:e} {c:.0f} {d:n} {e:.3f}", "cs", true)
        .message
    assertThat(result).isEqualTo(
      "{a, number, .000000} {b, number, scientific} {c, number} {d, number} {e, number, .000}",
    )
  }

  @Test
  fun testPythonBraceAutoIndexingAndPositional() {
    val result =
      getPythonBraceConvertor()
        .convert("{} {} {0} {1}", "cs", true)
        .message
    assertThat(result).isEqualTo("{0} {1} {0} {1}")
  }

  @Test
  fun testPythonBraceLiteralBraces() {
    val result =
      getPythonBraceConvertor()
        .convert("{{ {name} }}", "cs", true)
        .message
    assertThat(result).isEqualTo("'{' {name} '}'")
  }

  @Test
  fun testPythonBracePlurals() {
    val result =
      getPythonBraceConvertor()
        .convert(
          rawData =
            mapOf(
              0 to "Petr má jednoho psa.",
              1 to "Petr má {count:d} psi.",
              2 to "Petr má {count:d} psů.",
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
  fun `python brace round-trips supported placeholders symmetrically`() {
    val source = "{name} {count:d} {price:.2f} {ratio:e} {avg:f}"
    val exportedOnce = exportPythonBrace(importPythonBrace(source))
    // every supported placeholder maps back to its exact source form
    assertThat(exportedOnce).isEqualTo(source)
  }

  @Test
  fun `python brace round-trips unsupported placeholders and literal braces without corruption`() {
    val source = "{name} {count:d} {x!r} {y:>10} {{lit}}"
    val exportedOnce = exportPythonBrace(importPythonBrace(source))
    val exportedTwice = exportPythonBrace(importPythonBrace(exportedOnce))

    assertThat(exportedOnce).isEqualTo("{name} {count:d} {{x!r}} {{y:>10}} {{lit}}")
    // unsupported placeholders / literal braces are neutralized to escaped literals on the first
    // pass, then stay byte-stable on every subsequent Tolgee import -> export round-trip.
    assertThat(exportedTwice).isEqualTo(exportedOnce)
  }

  @Test
  fun `python brace plural round-trips the ICU hash placeholder`() {
    val expectedIcu =
      "{0, plural,\n" +
        "one {# dog}\n" +
        "few {# dogs}\n" +
        "other {# dogs}\n" +
        "}"

    val icu =
      getPythonBraceConvertor()
        .convert(
          rawData = mapOf(0 to "{count:d} dog", 1 to "{count:d} dogs", 2 to "{count:d} dogs"),
          languageTag = "cs",
          convertPlaceholders = true,
        ).message
    assertThat(icu).isEqualTo(expectedIcu)

    val exportedForms =
      IcuToPoMessageConvertor(
        message = icu ?: "",
        placeholderConvertorFactory = { IcuToPythonBracePlaceholderConvertor() },
        languageTag = "cs",
        forceIsPlural = true,
      ).convert().formsResult ?: emptyList()
    // the ICU "#" exports to a positional integer placeholder, never leaking raw "#"
    assertThat(exportedForms).isNotEmpty
    assertThat(exportedForms).allSatisfy {
      assertThat(it).contains("{0:d}")
      assertThat(it).doesNotContain("#")
    }

    // and "{0:d}" as the first plural param re-imports back to the same ICU "#" form
    val reimported =
      getPythonBraceConvertor()
        .convert(
          rawData = mapOf(0 to "{0:d} dog", 1 to "{0:d} dogs", 2 to "{0:d} dogs"),
          languageTag = "cs",
          convertPlaceholders = true,
        ).message
    assertThat(reimported).isEqualTo(expectedIcu)
  }

  @Test
  fun `python brace keeps a literal hash distinct from the plural number in plurals`() {
    val icu =
      getPythonBraceConvertor()
        .convert(
          rawData = mapOf(0 to "{count:d} comment #", 1 to "{count:d} comments #", 2 to "{count:d} comments #"),
          languageTag = "cs",
          convertPlaceholders = true,
        ).message
    // the count becomes the ICU plural "#"; the literal "#" is ICU-escaped so it is not a second number
    assertThat(icu).isEqualTo(
      "{0, plural,\n" +
        "one {# comment '#'}\n" +
        "few {# comments '#'}\n" +
        "other {# comments '#'}\n" +
        "}",
    )

    val exportedForms =
      IcuToPoMessageConvertor(
        message = icu ?: "",
        placeholderConvertorFactory = { IcuToPythonBracePlaceholderConvertor() },
        languageTag = "cs",
        forceIsPlural = true,
      ).convert().formsResult ?: emptyList()
    // export restores both: the count as "{0:d}" and the literal "#" verbatim
    assertThat(exportedForms).isNotEmpty
    assertThat(exportedForms).allSatisfy {
      assertThat(it).contains("{0:d}")
      assertThat(it).endsWith(" #")
    }
  }

  private fun getPythonBraceConvertor() = PoToIcuMessageConvertor { PythonBraceToIcuPlaceholderConvertor() }

  private fun importPythonBrace(message: String) = getPythonBraceConvertor().convert(message, "en", true).message ?: ""

  private fun exportPythonBrace(icuMessage: String) =
    MessageConvertorFactory(
      message = icuMessage,
      forceIsPlural = false,
      isProjectIcuPlaceholdersEnabled = true,
    ) { IcuToPythonBracePlaceholderConvertor() }.create().convert().singleResult ?: ""

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
