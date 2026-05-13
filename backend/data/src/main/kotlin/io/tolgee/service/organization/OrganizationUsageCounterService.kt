package io.tolgee.service.organization

import io.tolgee.Metrics
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.repository.OrganizationUsageCounterRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.Date
import kotlin.math.abs

@Service
class OrganizationUsageCounterService(
  private val organizationUsageCounterRepository: OrganizationUsageCounterRepository,
  private val organizationStatsService: OrganizationStatsService,
  private val entityManager: EntityManager,
  private val tolgeeProperties: TolgeeProperties,
  private val metrics: Metrics,
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Apply incremental key / translation deltas to the organization's counter row.
   *
   * If no row exists yet (new org created after the backfill migration), the row is seeded
   * via the slow-query recount and the delta then applied on top.
   *
   * Runs in the caller's transaction — if the caller rolls back, the counter UPDATE rolls
   * back too. This avoids any dual-write problem.
   */
  @Transactional
  fun applyDelta(
    organizationId: Long,
    keyDelta: Long,
    translationDelta: Long,
  ) {
    if (!tolgeeProperties.orgCounter.enabled) return
    if (keyDelta == 0L && translationDelta == 0L) return

    val start = System.nanoTime()
    val now = Date()

    val updated = organizationUsageCounterRepository.applyDelta(organizationId, keyDelta, translationDelta, now)
    if (updated == 0) {
      // No counter row yet. Flush in-flight changes so the recount captures them, then seed
      // with the post-flush count. The seed already reflects this transaction's delta, so
      // no second applyDelta is needed (a retry would double-count).
      seedRowFromRecount(organizationId)
    }

    metrics.orgCounterApplyDeltaTimer.record(Duration.ofNanos(System.nanoTime() - start))
  }

  /**
   * Read the current counter values. If no counter row exists yet, falls back to the
   * slow-query recount and returns those values *without* inserting a row.
   *
   * Why not seed on read: callers in billing wrap this in `executeInNewTransaction` for
   * caching purposes, which means we run in a separate DB transaction and cannot see
   * uncommitted writes from the caller's outer transaction (e.g. a newly-created
   * organization). Inserting a counter row keyed to an as-yet-uncommitted org id would
   * trip the FK constraint. Seeding belongs to `applyDelta`, which runs in the original
   * transaction at BEFORE_COMMIT — by then the org is flushed and visible.
   *
   * When the counter is disabled via `tolgee.org-counter.enabled`, falls back to the
   * slow-query recount on every call (i.e. pre-counter behavior).
   */
  @Transactional
  fun getCounts(organizationId: Long): Counts {
    if (!tolgeeProperties.orgCounter.enabled) {
      return Counts(
        organizationStatsService.getKeyCount(organizationId),
        organizationStatsService.getTranslationCount(organizationId),
      )
    }
    val row = organizationUsageCounterRepository.findByOrganizationId(organizationId)
    if (row != null) {
      return Counts(row.keyCount, row.translationCount)
    }
    return Counts(
      organizationStatsService.getKeyCount(organizationId),
      organizationStatsService.getTranslationCount(organizationId),
    )
  }

  /**
   * Definitive recount via the slow query. Overwrites the counter and stamps
   * `last_reconciled_at`. Used by the reconciliation job and by boundary verification.
   */
  @Transactional
  fun forceRecompute(organizationId: Long): Counts {
    val keyCount = organizationStatsService.getKeyCount(organizationId)
    val translationCount = organizationStatsService.getTranslationCount(organizationId)
    setAbsoluteOrSeed(organizationId, keyCount, translationCount)
    return Counts(keyCount, translationCount)
  }

  /**
   * Reconcile one org: recount via slow query, compare with the stored counter, emit drift
   * metrics on mismatch, and heal the counter. Runs in its own transaction so one slow
   * recount doesn't block others in the reconciliation cycle.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun reconcileOne(organizationId: Long) {
    val recountStart = System.nanoTime()
    val recountKeys = organizationStatsService.getKeyCount(organizationId)
    val recountTranslations = organizationStatsService.getTranslationCount(organizationId)
    metrics.orgCounterReconciliationDurationTimer.record(
      Duration.ofNanos(System.nanoTime() - recountStart),
    )

    val existing = organizationUsageCounterRepository.findByOrganizationId(organizationId)
    if (existing != null) {
      emitDriftMetricsIfAny(
        organizationId,
        cachedKeys = existing.keyCount,
        cachedTranslations = existing.translationCount,
        recountKeys = recountKeys,
        recountTranslations = recountTranslations,
        source = "reconciliation",
      )
    }
    setAbsoluteOrSeed(organizationId, recountKeys, recountTranslations)
    metrics.orgCounterReconciliationOrgsProcessedCounter.increment()
  }

  /**
   * Read counter values, but if either count is at or above
   * `threshold * limit`, run a definitive slow-query recount and use that instead.
   *
   * A drifted counter near a limit never causes a wrongful 400: the recount has the final
   * word. Far below the limit, the recount never fires.
   *
   * `keyLimit` / `translationLimit` may be `null` (unlimited / unknown), in which case
   * boundary verification skips that dimension.
   */
  @Transactional
  fun getCountsWithBoundaryVerification(
    organizationId: Long,
    keyLimit: Long?,
    translationLimit: Long?,
  ): Counts {
    if (!tolgeeProperties.orgCounter.enabled) {
      // Counter disabled — getCounts already returns a fresh slow-query result, so the
      // boundary check would just re-run the same query. Skip the verification step.
      return getCounts(organizationId)
    }
    val cached = getCounts(organizationId)
    val threshold = tolgeeProperties.orgCounter.boundaryVerifyThreshold
    val nearKeyBoundary = keyLimit != null && keyLimit > 0 && cached.keys >= threshold * keyLimit
    val nearTranslationBoundary =
      translationLimit != null && translationLimit > 0 &&
        cached.translations >= threshold * translationLimit

    if (!nearKeyBoundary && !nearTranslationBoundary) {
      return cached
    }

    metrics.orgCounterBoundaryVerifyTriggeredCounter.increment()
    val recount =
      Counts(
        organizationStatsService.getKeyCount(organizationId),
        organizationStatsService.getTranslationCount(organizationId),
      )
    val drifted =
      emitDriftMetricsIfAny(
        organizationId,
        cachedKeys = cached.keys,
        cachedTranslations = cached.translations,
        recountKeys = recount.keys,
        recountTranslations = recount.translations,
        source = "boundary_verify",
      )
    if (drifted) {
      metrics.orgCounterBoundaryVerifyMismatchCounter.increment()
      setAbsoluteOrSeed(organizationId, recount.keys, recount.translations)
    }
    return recount
  }

  /**
   * Compares cached counts against a fresh recount; on mismatch emits drift metrics tagged
   * with `source` and logs a warning. Returns whether drift was detected.
   */
  private fun emitDriftMetricsIfAny(
    organizationId: Long,
    cachedKeys: Long,
    cachedTranslations: Long,
    recountKeys: Long,
    recountTranslations: Long,
    source: String,
  ): Boolean {
    val keyDrift = recountKeys - cachedKeys
    val translationDrift = recountTranslations - cachedTranslations
    if (keyDrift == 0L && translationDrift == 0L) return false

    metrics.orgCounterDriftDetected(source).increment()
    if (keyDrift != 0L) {
      metrics.orgCounterDriftMagnitude("keys").record(abs(keyDrift).toDouble())
    }
    if (translationDrift != 0L) {
      metrics.orgCounterDriftMagnitude("translations").record(abs(translationDrift).toDouble())
    }
    logger.warn(
      "Counter drift for org {} (source={}): keyDrift={}, translationDrift={}. Healing.",
      organizationId,
      source,
      keyDrift,
      translationDrift,
    )
    return true
  }

  /**
   * Writes absolute counter values, falling back to a seed insert if no row exists yet
   * (e.g. the row was deleted between the reconciliation scan and this update).
   */
  private fun setAbsoluteOrSeed(
    organizationId: Long,
    keyCount: Long,
    translationCount: Long,
  ) {
    val now = Date()
    val updated =
      organizationUsageCounterRepository.setAbsolute(
        organizationId,
        keyCount,
        translationCount,
        now,
        now,
      )
    if (updated == 0) {
      seedRow(organizationId, keyCount, translationCount, lastReconciledAt = now)
    }
  }

  private fun seedRowFromRecount(organizationId: Long) {
    // Hibernate auto-flushes pending changes before native queries (default FlushMode.AUTO),
    // so the recount captures this transaction's in-flight changes. The seed therefore
    // already reflects the delta — no follow-up applyDelta is needed.
    val keyCount = organizationStatsService.getKeyCount(organizationId)
    val translationCount = organizationStatsService.getTranslationCount(organizationId)
    seedRow(organizationId, keyCount, translationCount, lastReconciledAt = Date())
  }

  /**
   * Insert a counter row using a native UPSERT (ON CONFLICT DO NOTHING) so concurrent
   * seeders don't trip the PK constraint.
   *
   * Flushes pending Hibernate changes first so the FK target (`organization` row) is
   * visible to this native INSERT. Without the flush, a newly-created org in the same
   * transaction may not be in the DB yet when this runs (e.g. during initial-user setup
   * where a Key is created in a brand-new org).
   */
  private fun seedRow(
    organizationId: Long,
    keyCount: Long,
    translationCount: Long,
    lastReconciledAt: Date?,
  ) {
    entityManager.flush()
    entityManager
      .createNativeQuery(
        """
        INSERT INTO organization_usage_counter
          (organization_id, key_count, translation_count, last_reconciled_at, created_at, updated_at)
        VALUES
          (:orgId, :keyCount, :translationCount, :lastReconciledAt, :now, :now)
        ON CONFLICT (organization_id) DO NOTHING
        """,
      ).setParameter("orgId", organizationId)
      .setParameter("keyCount", keyCount)
      .setParameter("translationCount", translationCount)
      .setParameter("lastReconciledAt", lastReconciledAt)
      .setParameter("now", Date())
      .executeUpdate()
  }

  data class Counts(
    val keys: Long,
    val translations: Long,
  )
}
