package io.tolgee.component.machineTranslation

import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.time.ExperimentalTime

@Component
class MtEventListener(
  private val mtCreditsService: MtCreditsService,
) {
  @EventListener(OnBeforeMachineTranslationEvent::class)
  @ExperimentalTime
  fun onBeforeMtEvent(event: OnBeforeMachineTranslationEvent) {
    mtCreditsService.checkPositiveBalance(event.organizationId)
  }

  @EventListener(OnAfterMachineTranslationEvent::class)
  fun onAfterMtEvent(event: OnAfterMachineTranslationEvent) {
    mtCreditsService.consumeCredits(event.organizationId, event.actualSumPriceInCents)
  }
}
