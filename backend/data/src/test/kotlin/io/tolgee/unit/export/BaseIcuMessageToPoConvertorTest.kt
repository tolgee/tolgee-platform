package io.tolgee.unit.export

import io.tolgee.service.export.exporters.BaseIcuMessageToPoConvertor
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class BaseIcuMessageToPoConvertorTest {
  @Test
  fun `converts simple message`() {
    BaseIcuMessageToPoConvertor("Hello {hello} {hello, number} {hello, number, .00}")
      .convert().result.assert.isEqualTo("Hello %s %s %s")
  }

  @Test
  fun `converts with plurals`() {
    val forms =
      BaseIcuMessageToPoConvertor("{0, plural, one {# dog} other {# dogs}}")
        .convert().forms

    forms!![0].assert.isEqualTo("%d dog")
    forms[1].assert.isEqualTo("%d dogs")
  }

  @Test
  fun `converts czech with plurals`() {
    val forms =
      BaseIcuMessageToPoConvertor(
        message = "{0, plural, one {# pes} few {# psi} other {# psů}}",
        languageTag = "cs",
      ).convert().forms

    forms!![0].assert.isEqualTo("%d pes")
    forms[1].assert.isEqualTo("%d psi")
    forms[2].assert.isEqualTo("%d psů")
  }
}
