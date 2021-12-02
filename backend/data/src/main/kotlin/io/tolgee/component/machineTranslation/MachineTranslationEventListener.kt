package io.tolgee.component.machineTranslation

import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.service.machineTranslation.MachineTranslationCreditBucketService
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class MachineTranslationEventListener(
  private val machineTranslationCreditBucketService: MachineTranslationCreditBucketService
) : ApplicationListener<OnBeforeMachineTranslationEvent> {
  override fun onApplicationEvent(event: OnBeforeMachineTranslationEvent) {
    machineTranslationCreditBucketService.consumeCredits(event.project, event.sumPrice)
  }
}
