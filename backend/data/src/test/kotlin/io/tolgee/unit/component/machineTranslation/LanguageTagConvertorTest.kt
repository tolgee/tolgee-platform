package io.tolgee.unit.component.machineTranslation

import io.tolgee.component.machineTranslation.LanguageTagConvertor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LanguageTagConvertorTest {
  @Test
  fun `it converts correctly`() {
    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("cs", "en", "de"), "cs-CZ_80"))
      .isEqualTo("cs")

    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("cs", "en", "de"), "en-US"))
      .isEqualTo("en")

    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("cs", "en", "de", "zh-TW"), "bla"))
      .isEqualTo(null)
  }

  @Test
  fun `it matches Chinese script subtags directly when provider supports them`() {
    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("zh", "zh-Hans", "zh-Hant"), "zh-Hant"))
      .isEqualTo("zh-Hant")
    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("zh", "zh-Hans", "zh-Hant"), "zh-Hans"))
      .isEqualTo("zh-Hans")
  }

  @Test
  fun `it matches case-insensitively to tolerate non-canonical provider lists`() {
    // BCP-47 tags are case-insensitive by spec; we should not silently downgrade
    // "pt-BR" to "pt" just because a provider declared "pt-br" lowercase.
    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("pt", "pt-br"), "pt-BR"))
      .isEqualTo("pt-BR")
    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("zh", "zh-hant"), "zh-Hant"))
      .isEqualTo("zh-Hant")
  }
}
