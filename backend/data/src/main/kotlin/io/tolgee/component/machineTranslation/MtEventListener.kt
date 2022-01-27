package io.tolgee.component.machineTranslation

import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.service.machineTranslation.MtCreditBucketService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class MtEventListener(
  private val mtCreditBucketService: MtCreditBucketService
) {
  @EventListener(OnBeforeMachineTranslationEvent::class)
  fun onBeforeMtEvent(event: OnBeforeMachineTranslationEvent) {
    mtCreditBucketService.consumeCredits(event.project, event.expectedSumPrice)
  }

  @EventListener(OnAfterMachineTranslationEvent::class)
  fun onAfterMtEvent(event: OnAfterMachineTranslationEvent) {
    mtCreditBucketService.addCredits(event.project, event.expectedSumPrice - event.actualSumPrice)
  }
}
