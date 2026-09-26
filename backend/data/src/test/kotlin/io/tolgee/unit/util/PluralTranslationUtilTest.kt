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

  @Test
  fun `escapes html special characters in forms that get the number tag`() {
    val baseString = """{number, plural, one {# < 5 & <b>more</b>} =1 {a < b & c} other {# items}}"""
    val result =
      PluralTranslationUtil.getPreparedSourceStrings(
        "en",
        "cs",
        getPluralFormsReplacingReplaceParam(baseString, PluralTranslationUtil.REPLACE_NUMBER_PLACEHOLDER)!!,
      )

    result.toMap()["one"].assert.isEqualTo("<x id=\"tolgee-number\">1</x> &lt; 5 &amp; &lt;b&gt;more&lt;/b&gt;")
    result.toMap()["=1"].assert.isEqualTo("a < b & c")
  }

  @Test
  fun `restores number placeholder and unescapes html in a translated form`() {
    PluralTranslationUtil
      .restoreNumberPlaceholder(
        "<x id=\"tolgee-number\">1</x> &lt; 5 &amp; &lt;b&gt;v&#237;c&lt;/b&gt;",
        htmlEscaped = true,
      ).assert
      .isEqualTo("# < 5 & <b>víc</b>")
  }

  @Test
  fun `does not unescape a translated form that was sent without the number tag`() {
    PluralTranslationUtil
      .restoreNumberPlaceholder("a &lt; b", htmlEscaped = false)
      .assert
      .isEqualTo("a &lt; b")
  }

  @Test
  fun `does not escape non-latin characters when adding the number tag`() {
    val baseString = """{number, plural, one {# élément für 5 € & 日本} other {# éléments}}"""
    val result =
      PluralTranslationUtil.getPreparedSourceStrings(
        "fr",
        "en",
        getPluralFormsReplacingReplaceParam(baseString, PluralTranslationUtil.REPLACE_NUMBER_PLACEHOLDER)!!,
      )

    result.toMap()["one"].assert.isEqualTo("<x id=\"tolgee-number\">1</x> élément für 5 € &amp; 日本")
  }

  @Test
  fun `escapes only the five markup characters and round-trips everything else`() {
    val sample =
      "it's \"quoted\" \u201Etypo\u201C \u2013 \u2026 50% \uD83D\uDE80 caf\u00E9 \u00DCber " +
        "\u65E5\u672C \u0645\u0631\u062D\u0628\u0627 \u041F\u0440\u0438\u0432\u0435\u0442 " +
        "nbsp\u00A0here tab\there {param} \\ / ~ `"
    val baseString = "{number, plural, one {# $sample} other {# items}}"
    val prepared =
      PluralTranslationUtil
        .getPreparedSourceStrings(
          "en",
          "cs",
          getPluralFormsReplacingReplaceParam(baseString, PluralTranslationUtil.REPLACE_NUMBER_PLACEHOLDER)!!,
        ).toMap()["one"]!!

    val expectedEscaped = sample.replace("\"", "&quot;").replace("'", "&#39;")
    prepared.assert.isEqualTo("<x id=\"tolgee-number\">1</x> $expectedEscaped")
    PluralTranslationUtil
      .restoreNumberPlaceholder(prepared, htmlEscaped = true)
      .assert
      .isEqualTo("# $sample")
  }

  @Test
  fun `a literal entity in the source survives the round trip`() {
    val baseString = "{number, plural, one {# items &amp; &lt;b&gt;} other {# items}}"
    val prepared =
      PluralTranslationUtil
        .getPreparedSourceStrings(
          "en",
          "cs",
          getPluralFormsReplacingReplaceParam(baseString, PluralTranslationUtil.REPLACE_NUMBER_PLACEHOLDER)!!,
        ).toMap()["one"]!!

    prepared.assert.isEqualTo("<x id=\"tolgee-number\">1</x> items &amp;amp; &amp;lt;b&amp;gt;")
    PluralTranslationUtil
      .restoreNumberPlaceholder(prepared, htmlEscaped = true)
      .assert
      .isEqualTo("# items &amp; &lt;b&gt;")
  }
}
