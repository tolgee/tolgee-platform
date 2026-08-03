package io.tolgee.unit

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider
import io.tolgee.service.LlmPropertiesService
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class LlmPropertiesServiceDefaultProviderTest {
  private fun createService(props: LlmProperties): LlmPropertiesService {
    return LlmPropertiesService(props, null)
  }

  private fun props(
    vararg providerNames: String,
    defaultProvider: String? = null,
    fallbacks: Map<String, String> = emptyMap(),
    disabled: Set<String> = emptySet(),
  ): LlmProperties {
    val props = LlmProperties()
    props.providers =
      providerNames
        .map { LlmProvider(name = it, enabled = it !in disabled) }
        .toMutableList()
    props.defaultProvider = defaultProvider
    props.fallbacks = fallbacks.toMutableMap()
    return props
  }

  @Test
  fun `returns configured default when it exists`() {
    val service = createService(props("gpt-4o", "claude", defaultProvider = "claude"))
    service.getDefaultProviderName().assert.isEqualTo("claude")
  }

  @Test
  fun `resolves renamed default through fallback chain`() {
    val service =
      createService(
        props(
          "gpt-6",
          defaultProvider = "gpt-4o",
          fallbacks = mapOf("gpt-4o" to "gpt-5", "gpt-5" to "gpt-6"),
        ),
      )
    service.getDefaultProviderName().assert.isEqualTo("gpt-6")
  }

  @Test
  fun `falls back to first provider when configured default is dangling`() {
    val service = createService(props("gpt-4o", "claude", defaultProvider = "removed-model"))
    service.getDefaultProviderName().assert.isEqualTo("gpt-4o")
  }

  @Test
  fun `falls back to first provider when fallback chain is circular`() {
    val service =
      createService(
        props(
          "gpt-4o",
          defaultProvider = "old-a",
          fallbacks = mapOf("old-a" to "old-b", "old-b" to "old-a"),
        ),
      )
    service.getDefaultProviderName().assert.isEqualTo("gpt-4o")
  }

  @Test
  fun `returns first provider when unset`() {
    val service = createService(props("gpt-4o", "claude"))
    service.getDefaultProviderName().assert.isEqualTo("gpt-4o")
  }

  @Test
  fun `returns null when no providers exist`() {
    val service = createService(props())
    service.getDefaultProviderName().assert.isNull()
  }

  @Test
  fun `ignores disabled configured default`() {
    val service =
      createService(props("gpt-4o", "claude", defaultProvider = "claude", disabled = setOf("claude")))
    service.getDefaultProviderName().assert.isEqualTo("gpt-4o")
  }
}
