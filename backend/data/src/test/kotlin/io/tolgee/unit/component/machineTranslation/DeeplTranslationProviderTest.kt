package io.tolgee.unit.component.machineTranslation

import io.tolgee.component.machineTranslation.providers.DeeplApiService
import io.tolgee.component.machineTranslation.providers.DeeplTranslationProvider
import io.tolgee.configuration.tolgee.machineTranslation.DeeplMachineTranslationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class DeeplTranslationProviderTest {
  private val provider =
    DeeplTranslationProvider(
      DeeplMachineTranslationProperties(),
      mock<DeeplApiService>(),
    )

  @Test
  fun `English regional variants resolve to en as source`() {
    assertThat(provider.getSuitableSourceTag("en-GB")).isEqualTo("en")
    assertThat(provider.getSuitableSourceTag("en-US")).isEqualTo("en")
  }

  @Test
  fun `Portuguese regional variants resolve to pt as source`() {
    assertThat(provider.getSuitableSourceTag("pt-PT")).isEqualTo("pt")
    assertThat(provider.getSuitableSourceTag("pt-BR")).isEqualTo("pt")
  }

  @Test
  fun `Chinese script variants resolve to zh as source`() {
    assertThat(provider.getSuitableSourceTag("zh-Hans")).isEqualTo("zh")
    assertThat(provider.getSuitableSourceTag("zh-Hant")).isEqualTo("zh")
  }

  @Test
  fun `target-only variants fall back to base tag as source`() {
    assertThat(provider.getSuitableSourceTag("de-CH")).isEqualTo("de")
    assertThat(provider.getSuitableSourceTag("fr-CA")).isEqualTo("fr")
    assertThat(provider.getSuitableSourceTag("es-419")).isEqualTo("es")
  }

  @Test
  fun `regional variants keep their region as target`() {
    assertThat(provider.getSuitableTag("en-GB")).isEqualTo("en-GB")
    assertThat(provider.getSuitableTag("pt-BR")).isEqualTo("pt-BR")
    assertThat(provider.getSuitableTag("zh-Hant")).isEqualTo("zh-Hant")
    assertThat(provider.getSuitableTag("de-CH")).isEqualTo("de-CH")
    assertThat(provider.getSuitableTag("fr-CA")).isEqualTo("fr-CA")
    assertThat(provider.getSuitableTag("es-419")).isEqualTo("es-419")
  }

  @Test
  fun `unregioned tags resolve identically for source and target`() {
    assertThat(provider.getSuitableSourceTag("en")).isEqualTo("en")
    assertThat(provider.getSuitableTag("en")).isEqualTo("en")
    assertThat(provider.getSuitableSourceTag("de")).isEqualTo("de")
    assertThat(provider.getSuitableTag("de")).isEqualTo("de")
  }

  @Test
  fun `extended non-beta languages are supported both ways`() {
    assertThat(provider.getSuitableSourceTag("he")).isEqualTo("he")
    assertThat(provider.getSuitableTag("he")).isEqualTo("he")
    assertThat(provider.getSuitableSourceTag("vi")).isEqualTo("vi")
    assertThat(provider.getSuitableTag("vi")).isEqualTo("vi")
    assertThat(provider.getSuitableSourceTag("th")).isEqualTo("th")
    assertThat(provider.getSuitableTag("th")).isEqualTo("th")
  }
}
