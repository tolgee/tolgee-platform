package io.tolgee.component.machineTranslation

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditBucketService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsConsumer
import jakarta.persistence.EntityManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import kotlin.time.ExperimentalTime

@Component
class MtEventListener(
  private val mtCreditBucketService: MtCreditBucketService,
  private val machineTranslationProperties: MachineTranslationProperties,
  private val publicBillingConfProvider: PublicBillingConfProvider,
  private val entityManager: EntityManager,
  private val mtCreditsConsumer: MtCreditsConsumer,
) {
  @EventListener(OnBeforeMachineTranslationEvent::class)
  @ExperimentalTime
  fun onBeforeMtEvent(event: OnBeforeMachineTranslationEvent) {
    mtCreditsConsumer.checkPositiveBalance(event.organizationId)
  }

  @EventListener(OnAfterMachineTranslationEvent::class)
  fun onAfterMtEvent(event: OnAfterMachineTranslationEvent) {
    mtCreditsConsumer.consumeCredits(event.organizationId, event.actualSumPrice)
  }
}
