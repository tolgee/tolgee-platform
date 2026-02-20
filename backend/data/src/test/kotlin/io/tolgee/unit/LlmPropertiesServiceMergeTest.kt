package io.tolgee.unit

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProviderDefaults
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.service.LlmPropertiesService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LlmPropertiesServiceMergeTest {
  private fun createService(props: LlmProperties): LlmPropertiesService {
    return LlmPropertiesService(props, null)
  }

  @Test
  fun `map-only config returns providers correctly`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "gpt-5-mini" to
          LlmProviderDefaults(
            type = LlmProviderType.OPENAI,
            model = "gpt-5-mini",
            apiUrl = "https://api.openai.com",
            tokenPriceInCreditsInput = 2.0,
            tokenPriceInCreditsOutput = 1.5,
          ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(1)
    assertThat(result[0].name).isEqualTo("gpt-5-mini")
    assertThat(result[0].model).isEqualTo("gpt-5-mini")
    assertThat(result[0].apiUrl).isEqualTo("https://api.openai.com")
    assertThat(result[0].tokenPriceInCreditsInput).isEqualTo(2.0)
    assertThat(result[0].tokenPriceInCreditsOutput).isEqualTo(1.5)
    assertThat(result[0].type).isEqualTo(LlmProviderType.OPENAI)
  }

  @Test
  fun `list-only config works unchanged (backward compat)`() {
    val props = LlmProperties()
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "my-provider",
          type = LlmProviderType.ANTHROPIC,
          apiKey = "sk-key",
          model = "claude-3",
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(1)
    assertThat(result[0].name).isEqualTo("my-provider")
    assertThat(result[0].type).isEqualTo(LlmProviderType.ANTHROPIC)
    assertThat(result[0].apiKey).isEqualTo("sk-key")
    assertThat(result[0].model).isEqualTo("claude-3")
  }

  @Test
  fun `merge - list apiKey fills into map defaults`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "gpt-5-mini" to
          LlmProviderDefaults(
            type = LlmProviderType.OPENAI,
            model = "gpt-5-mini",
            apiUrl = "https://api.openai.com",
            tokenPriceInCreditsInput = 2.0,
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "gpt-5-mini",
          apiKey = "sk-proj-secret",
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(1)
    assertThat(result[0].name).isEqualTo("gpt-5-mini")
    assertThat(result[0].apiKey).isEqualTo("sk-proj-secret")
    assertThat(result[0].model).isEqualTo("gpt-5-mini")
    assertThat(result[0].apiUrl).isEqualTo("https://api.openai.com")
    assertThat(result[0].tokenPriceInCreditsInput).isEqualTo(2.0)
  }

  @Test
  fun `merge - list null fields don't clobber map values`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "my-provider" to
          LlmProviderDefaults(
            model = "my-model",
            apiUrl = "https://my-api.com",
            format = "json_schema",
            priority = LlmProviderPriority.HIGH,
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "my-provider",
          apiKey = "secret",
          // model, apiUrl, format, priority are all null/default => should NOT override map
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(1)
    assertThat(result[0].model).isEqualTo("my-model")
    assertThat(result[0].apiUrl).isEqualTo("https://my-api.com")
    assertThat(result[0].format).isEqualTo("json_schema")
    assertThat(result[0].priority).isEqualTo(LlmProviderPriority.HIGH)
    assertThat(result[0].apiKey).isEqualTo("secret")
  }

  @Test
  fun `merge - list maxTokens overrides map when non-default`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "my-provider" to
          LlmProviderDefaults(
            maxTokens = 4000,
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "my-provider",
          maxTokens = 8000,
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(1)
    assertThat(result[0].maxTokens).isEqualTo(8000)
  }

  @Test
  fun `merge - list maxTokens default (2000) doesn't override map`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "my-provider" to
          LlmProviderDefaults(
            maxTokens = 4000,
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "my-provider",
          // maxTokens defaults to 2000
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(1)
    assertThat(result[0].maxTokens).isEqualTo(4000)
  }

  @Test
  fun `list-only provider (no map match) included in result`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "gpt-5-mini" to
          LlmProviderDefaults(
            model = "gpt-5-mini",
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "custom-only",
          type = LlmProviderType.ANTHROPIC,
          apiKey = "sk-key",
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(2)
    val customOnly = result.find { it.name == "custom-only" }
    assertThat(customOnly).isNotNull
    assertThat(customOnly!!.type).isEqualTo(LlmProviderType.ANTHROPIC)
    assertThat(customOnly.apiKey).isEqualTo("sk-key")
    val mapOnly = result.find { it.name == "gpt-5-mini" }
    assertThat(mapOnly).isNotNull
    assertThat(mapOnly!!.model).isEqualTo("gpt-5-mini")
  }

  @Test
  fun `disabled via map (enabled=false) excluded from getProviders`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "disabled-provider" to
          LlmProviderDefaults(
            enabled = false,
            model = "some-model",
          ),
      )

    val result = createService(props).getProviders()

    assertThat(result).isEmpty()
  }

  @Test
  fun `disabled via list override excluded from getProviders`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "my-provider" to
          LlmProviderDefaults(
            model = "some-model",
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "my-provider",
          enabled = false,
          apiKey = "key",
        ),
      )

    val result = createService(props).getProviders()

    assertThat(result).isEmpty()
  }

  @Test
  fun `multiple list entries with same name each merge against map defaults`() {
    val props = LlmProperties()
    props.providerDefaults =
      mutableMapOf(
        "gpt-5-mini" to
          LlmProviderDefaults(
            type = LlmProviderType.OPENAI,
            model = "gpt-5-mini",
            tokenPriceInCreditsInput = 2.0,
            tokenPriceInCreditsOutput = 1.5,
          ),
      )
    props.providers =
      mutableListOf(
        LlmProvider(
          name = "gpt-5-mini",
          apiKey = "sk-key-1",
          apiUrl = "https://api1.openai.com",
          priority = LlmProviderPriority.HIGH,
        ),
        LlmProvider(
          name = "gpt-5-mini",
          apiKey = "sk-key-2",
          apiUrl = "https://api2.openai.com",
          priority = LlmProviderPriority.LOW,
        ),
      )

    val result = createService(props).getMergedProviders()

    assertThat(result).hasSize(2)
    // Both should have the map defaults for model and prices
    assertThat(result[0].model).isEqualTo("gpt-5-mini")
    assertThat(result[0].tokenPriceInCreditsInput).isEqualTo(2.0)
    assertThat(result[0].apiKey).isEqualTo("sk-key-1")
    assertThat(result[0].apiUrl).isEqualTo("https://api1.openai.com")
    assertThat(result[0].priority).isEqualTo(LlmProviderPriority.HIGH)

    assertThat(result[1].model).isEqualTo("gpt-5-mini")
    assertThat(result[1].tokenPriceInCreditsOutput).isEqualTo(1.5)
    assertThat(result[1].apiKey).isEqualTo("sk-key-2")
    assertThat(result[1].apiUrl).isEqualTo("https://api2.openai.com")
    assertThat(result[1].priority).isEqualTo(LlmProviderPriority.LOW)
  }
}
