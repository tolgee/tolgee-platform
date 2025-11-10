package io.tolgee.service

import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider
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

  fun getProviders(): List<LlmProvider> {
    val result = llmProperties.providers.toMutableList()
    if (subscriptionActive()) {
      val hasTolgeeConfig = llmProperties.providers.find { it.type == LlmProviderType.TOLGEE } != null
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
}
