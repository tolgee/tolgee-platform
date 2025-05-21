package io.tolgee.service

import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider
import io.tolgee.model.enums.LlmProviderType
import org.springframework.stereotype.Service

@Service
class LlmPropertiesService(
  private val llmProperties: LlmProperties,
  private val eeSubscriptionProvider: EeSubscriptionProvider?
) {
  fun isEnabled(): Boolean {
    return llmProperties.enabled ?: (eeSubscriptionProvider?.findSubscriptionDto()?.licenseKey != null)
  }

  fun getProviders(): List<LlmProperties.LlmProvider> {
    return llmProperties.providers ?: listOf(
      LlmProvider(
        type = LlmProviderType.TOLGEE,
        name = "Tolgee",
        apiUrl = eeSubscriptionProvider?.getLicensingUrl() ?: "http://app.tolgee.io"
      )
    )
  }
}
