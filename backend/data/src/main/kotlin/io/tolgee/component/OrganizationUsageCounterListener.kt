package io.tolgee.component

import io.tolgee.configuration.TransactionScopeConfig
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.EntityPreCommitEvent
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationUsageCounterService
import io.tolgee.util.BypassableListener
import io.tolgee.util.getUsageIncreaseAmount
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Maintains the per-organization usage counter (`organization_usage_counter`) by accumulating
 * key / translation deltas as `EntityPreCommitEvent`s fire, then flushing them to the DB once
 * per transaction via a `beforeCommit` synchronization.
 *
 * One instance per transaction (`@Scope("transaction")`), so the delta maps and the
 * "synchronization registered" flag reset automatically between transactions.
 *
 * The flush runs inside the original transaction — if the transaction rolls back (e.g. a
 * limit check throws `BadRequestException`), the counter UPDATE rolls back with it. No
 * dual-write problem.
 */
@Scope(TransactionScopeConfig.SCOPE_TRANSACTION)
@Component
class OrganizationUsageCounterListener(
  private val counterService: OrganizationUsageCounterService,
  private val tolgeeProperties: TolgeeProperties,
) : BypassableListener {
  override var bypass: Boolean = false

  private val logger = LoggerFactory.getLogger(javaClass)

  private var organizationId: Long? = null
  private var keyDelta: Long = 0
  private var translationDelta: Long = 0
  private var synchronizationRegistered = false

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

    if (organizationId == null) {
      organizationId = orgId
    } else if (organizationId != orgId) {
      // No runtime path mutates keys/translations across multiple orgs in one
      // transaction — keys/translations always belong to a single project, which belongs
      // to a single org. If this ever fires, a new code path is doing something the
      // counter doesn't account for. Reconciliation will heal whatever we miss.
      logger.warn(
        "Counter listener saw events for multiple organizations in one transaction " +
          "(first={}, now={}). Skipping the second org; reconciliation will heal.",
        organizationId,
        orgId,
      )
      return
    }

    if (isKey) keyDelta += delta else translationDelta += delta
    registerSynchronizationOnce()
  }

  private fun registerSynchronizationOnce() {
    if (synchronizationRegistered) return
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      logger.warn(
        "Counter delta event fired outside an active transaction synchronization scope; " +
          "deltas will not be flushed. Reconciliation will heal this.",
      )
      return
    }
    synchronizationRegistered = true
    TransactionSynchronizationManager.registerSynchronization(
      object : TransactionSynchronization {
        override fun beforeCommit(readOnly: Boolean) {
          if (readOnly) return
          flushDelta()
        }
      },
    )
  }

  private fun flushDelta() {
    val orgId = organizationId ?: return
    counterService.applyDelta(orgId, keyDelta, translationDelta)
  }
}
