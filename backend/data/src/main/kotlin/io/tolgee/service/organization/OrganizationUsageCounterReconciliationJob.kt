package io.tolgee.service.organization

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
 */
@Component
class OrganizationUsageCounterReconciliationJob(
  private val organizationUsageCounterService: OrganizationUsageCounterService,
  private val organizationUsageCounterRepository: OrganizationUsageCounterRepository,
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  @Scheduled(cron = "\${tolgee.org-counter.reconciliation-cron:0 0 3 * * *}")
  fun reconcile() {
    if (!tolgeeProperties.orgCounter.enabled) return
    runSentryCatching {
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
}
