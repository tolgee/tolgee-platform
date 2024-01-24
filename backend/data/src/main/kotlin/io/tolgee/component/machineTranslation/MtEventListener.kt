package io.tolgee.component.machineTranslation

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.model.MtCreditBucket
import io.tolgee.service.machineTranslation.MtCreditBucketService
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
) {
  @EventListener(OnBeforeMachineTranslationEvent::class)
  @ExperimentalTime
  fun onBeforeMtEvent(event: OnBeforeMachineTranslationEvent) {
    if (shouldConsumeCredits()) {
      mtCreditBucketService.checkPositiveBalance(event.organizationId)
    }
  }

  @EventListener(OnAfterMachineTranslationEvent::class)
  fun onAfterMtEvent(event: OnAfterMachineTranslationEvent) {
    if (shouldConsumeCredits()) {
      val bucket = mtCreditBucketService.consumeCredits(event.organizationId, event.actualSumPrice)
      detachBucketAfterConsumption(bucket)
    }
  }

  /**
   * Since consumption does happen in a separate transaction,
   * we need to detach the bucket from the current transaction
   *
   * Otherwise, hibernate saves the entity and credits won't be consumed
   */
  private fun detachBucketAfterConsumption(bucket: MtCreditBucket) {
    val bucketRef = entityManager.getReference(MtCreditBucket::class.java, bucket.id)
    entityManager.detach(bucketRef)
  }

  fun shouldConsumeCredits(): Boolean {
    return machineTranslationProperties.freeCreditsAmount > -1 || publicBillingConfProvider().enabled
  }
}
