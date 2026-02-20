package io.tolgee.service

import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProviderDefaults
import io.tolgee.exceptions.InvalidStateException
import io.tolgee.model.enums.LlmProviderType
import org.springframework.stereotype.Service

@Service
class LlmPropertiesService(
  private val llmProperties: LlmProperties,
  private val eeSubscriptionProvider: EeSubscriptionProvider?,
) {
  fun subscriptionActive(): Boolean {
    return eeSubscriptionProvider?.findSubscriptionDto()?.licenseKey != null
  }

  fun isEnabled(): Boolean {
    return llmProperties.enabled ?: getProviders().isNotEmpty()
  }

  fun getFallbackProviderName(providerName: String): String? {
    return llmProperties.fallbacks[providerName]
  }

  fun getProviders(): List<LlmProvider> {
    val result = getMergedProviders().toMutableList()
    if (subscriptionActive()) {
      val hasTolgeeConfig = result.find { it.type == LlmProviderType.TOLGEE } != null
      if (!hasTolgeeConfig) {
        result.add(
          LlmProvider(
            type = LlmProviderType.TOLGEE,
            name = "Tolgee",
            apiUrl = eeSubscriptionProvider?.getLicensingUrl() ?: throw InvalidStateException(),
          ),
        )
      }
    }
    return result.filter { it.enabled }
  }

  fun getMergedProviders(): List<LlmProvider> {
    val defaults = llmProperties.providerDefaults
    if (defaults.isEmpty()) {
      return llmProperties.providers.toList()
    }

    val result = mutableListOf<LlmProvider>()
    val matchedDefaultNames = mutableSetOf<String>()

    for (listEntry in llmProperties.providers) {
      val mapEntry = defaults[listEntry.name]
      if (mapEntry != null) {
        matchedDefaultNames.add(listEntry.name)
        result.add(mergeProviderWithDefaults(mapEntry, listEntry))
      } else {
        result.add(listEntry)
      }
    }

    // Add map-only entries not matched by any list entry
    for ((name, mapEntry) in defaults) {
      if (name !in matchedDefaultNames) {
        result.add(mapEntry.toLlmProvider(name))
      }
    }

    return result
  }

  private fun mergeProviderWithDefaults(
    defaults: LlmProviderDefaults,
    listEntry: LlmProvider,
  ): LlmProvider {
    val base = defaults.toLlmProvider(listEntry.name)
    // enabled is always taken from the list entry
    base.enabled = listEntry.enabled
    // Nullable fields: override only if list value is non-null
    listEntry.apiKey?.let { base.apiKey = it }
    listEntry.apiUrl?.let { base.apiUrl = it }
    listEntry.model?.let { base.model = it }
    listEntry.deployment?.let { base.deployment = it }
    listEntry.reasoningEffort?.let { base.reasoningEffort = it }
    listEntry.format?.let { base.format = it }
    listEntry.priority?.let { base.priority = it }
    listEntry.attempts?.let { base.attempts = it }
    listEntry.tokenPriceInCreditsInput?.let { base.tokenPriceInCreditsInput = it }
    listEntry.tokenPriceInCreditsOutput?.let { base.tokenPriceInCreditsOutput = it }
    // Non-nullable fields: override only if different from default
    if (listEntry.type != LlmProviderType.OPENAI) {
      base.type = listEntry.type
    }
    if (listEntry.maxTokens != LlmProvider.MAX_TOKENS_DEFAULT) {
      base.maxTokens = listEntry.maxTokens
    }
    return base
  }
}
