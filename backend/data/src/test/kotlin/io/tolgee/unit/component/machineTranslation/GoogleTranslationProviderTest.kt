package io.tolgee.unit.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.GoogleTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.GoogleMachineTranslationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GoogleTranslationProviderTest {
  private val provider = GoogleTranslationProvider(GoogleMachineTranslationProperties(), null)

  @Test
  fun `maps zh-Hant to zh-TW (NMT does not recognize zh-Hant)`() {
    assertThat(provider.getSuitableTag("zh-Hant")).isEqualTo("zh-TW")
  }

  @Test
  fun `maps zh-Hans to zh-CN (NMT does not recognize zh-Hans)`() {
    assertThat(provider.getSuitableTag("zh-Hans")).isEqualTo("zh-CN")
  }

  @Test
  fun `maps Chinese script subtags case-insensitively`() {
    assertThat(provider.getSuitableTag("zh-hant")).isEqualTo("zh-TW")
    assertThat(provider.getSuitableTag("ZH-HANT")).isEqualTo("zh-TW")
    assertThat(provider.getSuitableTag("zh-hans")).isEqualTo("zh-CN")
  }

  @Test
  fun `non-Chinese tags resolve normally`() {
    assertThat(provider.getSuitableTag("en-US")).isEqualTo("en")
    assertThat(provider.getSuitableTag("de")).isEqualTo("de")
  }
}
