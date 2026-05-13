package io.tolgee.component

import io.tolgee.configuration.TransactionScopeConfig
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationUsageCounterService
import io.tolgee.util.BypassableListener
import io.tolgee.util.getUsageIncreaseAmount
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Scope
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Maintains the per-organization usage counter (`organization_usage_counter`) by accumulating
 * key / translation deltas as `EntityPreCommitEvent`s fire, then flushing them to the DB
 * once per transaction.
 *
 * One instance per transaction (`@Scope("transaction")`), so all state resets automatically
 * between transactions. The flush is triggered by publishing `OrgCounterFlushSignal` on
 * the first delta-bearing event of the transaction; Spring's `@TransactionalEventListener`
 * fires the handler at `BEFORE_COMMIT`, inside the original transaction — if the
 * transaction rolls back, the counter UPDATE rolls back with it.
 */
@Scope(TransactionScopeConfig.SCOPE_TRANSACTION)
@Component
class OrganizationUsageCounterListener(
  private val counterService: OrganizationUsageCounterService,
  private val tolgeeProperties: TolgeeProperties,
  private val applicationEventPublisher: ApplicationEventPublisher,
) : BypassableListener {
  override var bypass: Boolean = false

  private var organizationId: Long? = null
  private var keyDelta: Long = 0
  private var translationDelta: Long = 0
  private var flushSignalPublished = false

  @EventListener
  fun onPreCommit(event: EntityPreCommitEvent) {
    if (!tolgeeProperties.orgCounter.enabled || bypass) return

    val delta = event.getUsageIncreaseAmount()
    if (delta == 0L) return

    val (orgId, isKey) =
      when (val entity = event.entity) {
        is Key -> entity.project.organizationOwner.id to true
        is Translation -> entity.key.project.organizationOwner.id to false
        else -> return
      }

    // A single transaction only ever mutates keys/translations under one org — every
    // such entity reaches us via a single project, which belongs to a single org.
    organizationId = orgId
    if (isKey) keyDelta += delta else translationDelta += delta

    if (!flushSignalPublished) {
      flushSignalPublished = true
      applicationEventPublisher.publishEvent(OrgCounterFlushSignal)
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun flushDelta(signal: OrgCounterFlushSignal) {
    val orgId = organizationId ?: return
    counterService.applyDelta(orgId, keyDelta, translationDelta)
  }

  /** Per-transaction signal that there are counter deltas to flush at BEFORE_COMMIT. */
  object OrgCounterFlushSignal
}
