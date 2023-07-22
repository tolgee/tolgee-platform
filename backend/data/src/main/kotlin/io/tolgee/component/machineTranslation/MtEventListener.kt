package io.tolgee.component.machineTranslation

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.service.machineTranslation.MtCreditBucketService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MtEventListener(
  private val mtCreditBucketService: MtCreditBucketService,
  private val machineTranslationProperties: MachineTranslationProperties,
  private val publicBillingConfProvider: PublicBillingConfProvider
) {
  @EventListener(OnBeforeMachineTranslationEvent::class)
  fun onBeforeMtEvent(event: OnBeforeMachineTranslationEvent) {
    if (shouldConsumeCredits()) {
      mtCreditBucketService.checkPositiveBalance(event.project)
    }
  }

  @EventListener(OnAfterMachineTranslationEvent::class)
  fun onAfterMtEvent(event: OnAfterMachineTranslationEvent) {
    if (shouldConsumeCredits()) {
      mtCreditBucketService.consumeCredits(event.project, event.actualSumPrice)
    }
  }

  fun shouldConsumeCredits(): Boolean {
    return machineTranslationProperties.freeCreditsAmount > -1 || publicBillingConfProvider().enabled
  }
}
