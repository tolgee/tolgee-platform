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

    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("cs", "en", "de", "zh-TW"), "zh-Hant"))
      .isEqualTo("zh-TW")

    assertThat(LanguageTagConvertor.findSuitableTag(arrayOf("cs", "en", "de", "zh-TW"), "bla"))
      .isEqualTo(null)
  }
}
