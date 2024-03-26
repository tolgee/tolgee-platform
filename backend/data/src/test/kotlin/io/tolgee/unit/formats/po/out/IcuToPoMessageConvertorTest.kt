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
  fun `converts with plurals`() {
    val forms =
      IcuToPoMessageConvertor(
        "{0, plural, one {# dog} other {# dogs}}",
        IcuToPhpPlaceholderConvertor(),
        forceIsPlural = true,
      )
        .convert().formsResult

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
