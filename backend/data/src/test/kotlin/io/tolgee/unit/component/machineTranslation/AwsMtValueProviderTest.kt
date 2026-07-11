package io.tolgee.unit.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.AwsMtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.AwsMachineTranslationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AwsMtValueProviderTest {
  private val provider = AwsMtValueProvider(AwsMachineTranslationProperties(), null)

  @Test
  fun `maps zh-Hant to zh-TW (AWS has no zh-Hant)`() {
    assertThat(provider.getSuitableTag("zh-Hant")).isEqualTo("zh-TW")
  }

  @Test
  fun `maps zh-Hant case-insensitively`() {
    assertThat(provider.getSuitableTag("zh-hant")).isEqualTo("zh-TW")
    assertThat(provider.getSuitableTag("ZH-HANT")).isEqualTo("zh-TW")
  }

  @Test
  fun `zh-Hans falls through to zh (Simplified)`() {
    assertThat(provider.getSuitableTag("zh-Hans")).isEqualTo("zh")
  }

  @Test
  fun `non-Chinese tags resolve normally`() {
    assertThat(provider.getSuitableTag("en-US")).isEqualTo("en")
    assertThat(provider.getSuitableTag("de")).isEqualTo("de")
  }
}
