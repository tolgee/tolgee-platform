package io.tolgee.unit.formats.po.out

import io.tolgee.formats.po.out.BaseIcuMessageToPoConvertor
import io.tolgee.formats.po.out.php.PhpFromIcuParamConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class BaseIcuMessageToPoConvertorTest {
  @Test
  fun `converts simple message`() {
    BaseIcuMessageToPoConvertor(
      "Hello {hello} {hello, number} {hello, number, .00}",
      PhpFromIcuParamConvertor(),
      forceIsPlural = false,
    ).convert().singleResult.assert.isEqualTo("Hello %s %d %.2f")
  }

  @Test
  fun `converts with plurals`() {
    val forms =
      BaseIcuMessageToPoConvertor(
        "{0, plural, one {# dog} other {# dogs}}",
        PhpFromIcuParamConvertor(),
        forceIsPlural = true,
      )
        .convert().formsResult

    forms!![0].assert.isEqualTo("%d dog")
    forms[1].assert.isEqualTo("%d dogs")
  }

  @Test
  fun `converts czech with plurals`() {
    val forms =
      BaseIcuMessageToPoConvertor(
        message = "{0, plural, one {# pes} few {# psi} other {# psů}}",
        languageTag = "cs",
        argumentConverter = PhpFromIcuParamConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psi")
    forms[2].assert.isEqualTo("%d psů")
  }

  @Test
  fun `fallbacks to other`() {
    val forms =
      BaseIcuMessageToPoConvertor(
        message = "{0, plural, one {# pes} other {# psů}}",
        languageTag = "cs",
        argumentConverter = PhpFromIcuParamConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psů")
    forms[2].assert.isEqualTo("%d psů")
  }

  @Test
  fun `fallbacks works when unsupported form is present other`() {
    val forms =
      BaseIcuMessageToPoConvertor(
        message = "{0, plural, one {# pes} many {# pesos} other {# psů}}",
        languageTag = "cs",
        argumentConverter = PhpFromIcuParamConvertor(),
        forceIsPlural = true,
      ).convert().formsResult

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psů")
    forms[2].assert.isEqualTo("%d psů")
  }
}
