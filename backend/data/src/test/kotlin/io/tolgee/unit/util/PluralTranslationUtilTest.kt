package io.tolgee.unit.util

import io.tolgee.formats.getPluralFormsReplacingReplaceParam
import io.tolgee.service.machineTranslation.PluralTranslationUtil
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class PluralTranslationUtilTest {
  @Test
  fun `provides correct forms for basic MT providers`() {
    val baseString = """{number, plural, one {# apple} =1 {one apple} =2 {Two apples} =5 {# apples} other {# apples}}"""
    val result =
      PluralTranslationUtil.getPreparedSourceStrings(
        "en",
        "cs",
        getPluralFormsReplacingReplaceParam(baseString, PluralTranslationUtil.REPLACE_NUMBER_PLACEHOLDER)!!,
      )

    result.toMap().assert.isEqualTo(
      mapOf(
        "one" to "<x id=\"tolgee-number\">1</x> apple",
        "few" to "<x id=\"tolgee-number\">2</x> apples",
        "many" to "<x id=\"tolgee-number\">0.5</x> apples",
        "other" to "<x id=\"tolgee-number\">10</x> apples",
        "=1" to "one apple",
        "=2" to "Two apples",
        "=5" to "<x id=\"tolgee-number\">5</x> apples",
      ),
    )
  }
}
