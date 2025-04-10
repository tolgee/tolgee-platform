package io.tolgee.unit.formats.po.out

import io.tolgee.formats.paramConvertors.out.IcuToPhpPlaceholderConvertor
import io.tolgee.formats.po.out.IcuToPoMessageConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class IcuToPoMessageConvertorTest {
  @Test
  fun `converts simple message`() {
    IcuToPoMessageConvertor(
      "Hello {hello} {hello, number} {hello, number, .00}",
      IcuToPhpPlaceholderConvertor(),
      forceIsPlural = false,
    ).convert().singleResult.assert.isEqualTo("Hello %s %d %.2f")
  }

  @Test
  fun `converts message with complex escape sequences`() {
    IcuToPoMessageConvertor(
      "Normal hashtag # and escaped hashtag '#' and double escaped hashtag '''#'''",
      IcuToPhpPlaceholderConvertor(),
      forceIsPlural = false,
    ).convert().singleResult.assert.isEqualTo(
      "Normal hashtag # and escaped hashtag '#' and double escaped hashtag ''#''",
    )
  }

  @Test
  fun `converts message with complex escape sequences and icu disabled`() {
    IcuToPoMessageConvertor(
      "Normal hashtag # and escaped hashtag '#' and double escaped hashtag '''#'''",
      IcuToPhpPlaceholderConvertor(),
      forceIsPlural = false,
      projectIcuPlaceholdersSupport = false,
    ).convert().singleResult.assert.isEqualTo(
      "Normal hashtag # and escaped hashtag '#' and double escaped hashtag '''#'''",
    )
  }

  @Test
  fun `converts with plurals`() {
    val forms =
      IcuToPoMessageConvertor(
        "{0, plural, one {# dog} other {# dogs}}",
        IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
      ).convert()
        .formsResult

    forms!![0].assert.isEqualTo("%d dog")
    forms[1].assert.isEqualTo("%d dogs")
  }

  @Test
  fun `converts czech with plurals`() {
    val forms =
      IcuToPoMessageConvertor(
        message = "{0, plural, one {# pes} few {# psi} other {# psů}}",
        languageTag = "cs",
        placeholderConvertor = IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psi")
    forms[2].assert.isEqualTo("%d psů")
  }

  @Test
  fun `converts complex escape sequences with plurals`() {
    val forms =
      IcuToPoMessageConvertor(
        message =
          "{0, plural," +
            " one {# znak '#' pro psa}" +
            " few {# znaky '#' pro psi}" +
            " other {# znaků '''''#''''' a '#' pro psi}" +
            "}",
        languageTag = "cs",
        placeholderConvertor = IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d znak # pro psa")
    forms[1].assert.isEqualTo("%d znaky # pro psi")
    forms[2].assert.isEqualTo("%d znaků ''#'' a # pro psi")
  }

  @Test
  fun `converts complex escape sequences with plurals and icu disabled`() {
    val forms =
      IcuToPoMessageConvertor(
        message =
          "{0, plural," +
            " one {'#' znak '#' pro psa}" +
            " few {'#' znaky '#' pro psi}" +
            " other {'#' znaků '''''#''''' a '#' pro psi}" +
            "}",
        languageTag = "cs",
        placeholderConvertor = IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
        projectIcuPlaceholdersSupport = false,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("# znak # pro psa")
    forms[1].assert.isEqualTo("# znaky # pro psi")
    forms[2].assert.isEqualTo("# znaků ''#'' a # pro psi")
  }

  @Test
  fun `fallbacks to other`() {
    val forms =
      IcuToPoMessageConvertor(
        message = "{0, plural, one {# pes} other {# psů}}",
        languageTag = "cs",
        placeholderConvertor = IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psů")
    forms[2].assert.isEqualTo("%d psů")
  }

  @Test
  fun `fallbacks works when unsupported form is present other`() {
    val forms =
      IcuToPoMessageConvertor(
        message = "{0, plural, one {# pes} many {# pesos} other {# psů}}",
        languageTag = "cs",
        placeholderConvertor = IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psů")
    forms[2].assert.isEqualTo("%d psů")
  }
}
