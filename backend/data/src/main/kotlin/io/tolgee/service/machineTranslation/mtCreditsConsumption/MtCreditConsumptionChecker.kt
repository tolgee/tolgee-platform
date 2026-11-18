package io.tolgee.service.machineTranslation.mtCreditsConsumption

import io.tolgee.component.machineTranslation.providers.LlmTranslationProvider
import io.tolgee.constants.MtServiceType
import org.springframework.stereotype.Component

@Component
class MtCreditConsumptionChecker(
  private val llmTranslationProvider: LlmTranslationProvider,
) {
  fun consumesCredits(
    organizationId: Long,
    services: Set<MtServiceType>,
    llmProviders: Set<String>,
  ): Boolean {
    if (services.any { it != MtServiceType.PROMPT }) return true
    return llmProviders.any { llmTranslationProvider.consumesCredits(organizationId, it) }
  }
}
