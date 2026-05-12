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

  private val organizationKeyDeltas: MutableMap<Long, Long> = mutableMapOf()
  private val organizationTranslationDeltas: MutableMap<Long, Long> = mutableMapOf()
  private var synchronizationRegistered = false

  @EventListener
  fun onPreCommit(event: EntityPreCommitEvent) {
    if (!tolgeeProperties.orgCounter.enabled || bypass) return

    val delta = event.getUsageIncreaseAmount()
    if (delta == 0L) return

    when (val entity = event.entity) {
      is Key -> {
        val orgId = entity.project.organizationOwner.id
        organizationKeyDeltas.merge(orgId, delta, Long::plus)
        registerSynchronizationOnce()
      }
      is Translation -> {
        val orgId = entity.key.project.organizationOwner.id
        organizationTranslationDeltas.merge(orgId, delta, Long::plus)
        registerSynchronizationOnce()
      }
    }
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
          flushDeltas()
        }
      },
    )
  }

  private fun flushDeltas() {
    val orgIds = organizationKeyDeltas.keys + organizationTranslationDeltas.keys
    orgIds.forEach { orgId ->
      val keyDelta = organizationKeyDeltas[orgId] ?: 0
      val translationDelta = organizationTranslationDeltas[orgId] ?: 0
      counterService.applyDelta(orgId, keyDelta, translationDelta)
    }
  }
}
