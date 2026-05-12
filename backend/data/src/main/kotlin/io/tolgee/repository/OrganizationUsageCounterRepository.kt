package io.tolgee.repository

import io.tolgee.model.OrganizationUsageCounter
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface OrganizationUsageCounterRepository : JpaRepository<OrganizationUsageCounter, Long> {
  /**
   * Atomic per-row UPDATE that stacks deltas onto the existing counter values.
   *
   * Postgres serializes concurrent updates against the same row via row-level locking,
   * so concurrent commits adding/removing keys or translations stack their deltas
   * without OptimisticLockException.
   *
   * Returns the number of rows updated (0 = no counter row yet → caller must seed).
   */
  @Modifying
  @Query(
    value = """
      UPDATE organization_usage_counter
      SET key_count = key_count + :keyDelta,
          translation_count = translation_count + :translationDelta,
          updated_at = :now
      WHERE organization_id = :organizationId
    """,
    nativeQuery = true,
  )
  fun applyDelta(
    @Param("organizationId") organizationId: Long,
    @Param("keyDelta") keyDelta: Long,
    @Param("translationDelta") translationDelta: Long,
    @Param("now") now: Date,
  ): Int

  /**
   * Set the counter to absolute values (used by reconciliation and forceRecompute).
   */
  @Modifying
  @Query(
    value = """
      UPDATE organization_usage_counter
      SET key_count = :keyCount,
          translation_count = :translationCount,
          updated_at = :now,
          last_reconciled_at = :lastReconciledAt
      WHERE organization_id = :organizationId
    """,
    nativeQuery = true,
  )
  fun setAbsolute(
    @Param("organizationId") organizationId: Long,
    @Param("keyCount") keyCount: Long,
    @Param("translationCount") translationCount: Long,
    @Param("now") now: Date,
    @Param("lastReconciledAt") lastReconciledAt: Date?,
  ): Int

  fun findByOrganizationId(organizationId: Long): OrganizationUsageCounter?
}
