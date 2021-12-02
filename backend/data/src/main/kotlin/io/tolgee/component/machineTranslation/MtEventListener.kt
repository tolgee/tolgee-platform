package io.tolgee.component.machineTranslation

import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.service.machineTranslation.MtCreditBucketService
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class MtEventListener(
  private val mtCreditBucketService: MtCreditBucketService
) : ApplicationListener<OnBeforeMachineTranslationEvent> {
  override fun onApplicationEvent(event: OnBeforeMachineTranslationEvent) {
    mtCreditBucketService.consumeCredits(event.project, event.sumPrice)
  }
}
