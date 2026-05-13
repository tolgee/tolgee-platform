package io.tolgee.service.organization

import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.repository.OrganizationUsageCounterRepository
import io.tolgee.util.Logging
import io.tolgee.util.logger
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Nightly reconciliation of the per-organization usage counter against the source-of-truth
 * slow query. Pages through orgs ordered by least-recently-reconciled, recounts each in
 * its own transaction, emits drift metrics on mismatch, and heals the counter.
 *
 * Bounded by `tolgee.org-counter.reconciliation-max-duration` per cycle — orgs not reached
 * roll over to the next cycle because the ordering is stable on `last_reconciled_at`.
 *
 * In a multi-pod deployment the `@Scheduled` fires on every pod simultaneously; the
 * distributed lock (Redisson when Redis is configured, in-process otherwise) ensures only
 * one pod actually runs the cycle. Other pods see the lock held and skip.
 */
@Component
class OrganizationUsageCounterReconciliationJob(
  private val organizationUsageCounterService: OrganizationUsageCounterService,
  private val organizationUsageCounterRepository: OrganizationUsageCounterRepository,
  private val tolgeeProperties: TolgeeProperties,
  private val lockingProvider: LockingProvider,
) : Logging {
  companion object {
    private const val LOCK_NAME = "org-usage-counter-reconciliation"
  }

  @Scheduled(cron = "\${tolgee.org-counter.reconciliation-cron:0 0 3 * * *}")
  fun reconcile() {
    if (!tolgeeProperties.orgCounter.enabled) return
    // Lease is the max-duration plus a small buffer so it expires automatically if a pod
    // dies mid-cycle. Other pods that arrive after expiry pick up where it left off
    // (orgs are ordered by least-recently-reconciled).
    val leaseTime = tolgeeProperties.orgCounter.reconciliationMaxDuration.plusMinutes(5)
    val ran =
      lockingProvider.withLockingIfFree(LOCK_NAME, leaseTime) {
        runSentryCatching {
          runCycle()
        }
      }
    if (ran == null) {
      logger.debug("Org usage counter reconciliation already running on another node — skipping")
    }
  }

  private fun runCycle() {
    val maxDurationNanos = tolgeeProperties.orgCounter.reconciliationMaxDuration.toNanos()
    val pageSize = tolgeeProperties.orgCounter.reconciliationPageSize
    val start = System.nanoTime()
    var processed = 0

    while (System.nanoTime() - start < maxDurationNanos) {
      val ids = organizationUsageCounterRepository.findOrgIdsForReconciliation(pageSize)
      if (ids.isEmpty()) break
      for (orgId in ids) {
        if (System.nanoTime() - start >= maxDurationNanos) break
        runSentryCatching {
          organizationUsageCounterService.reconcileOne(orgId)
          processed++
        }
      }
    }

    logger.info("Organization usage counter reconciliation processed $processed orgs")
  }
}
