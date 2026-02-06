package io.tolgee.repository.batch

import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository

/**
 * Repository for [OpenAiBatchJobTracker] entities.
 *
 * [findAndLockByStatusIn] uses `PESSIMISTIC_WRITE` with lock timeout `-2`
 * (PostgreSQL `SKIP LOCKED`) to allow concurrent poll cycles across instances
 * without blocking. Rows locked by another transaction are silently skipped.
 */
@Repository
@Lazy
interface OpenAiBatchJobTrackerRepository : JpaRepository<OpenAiBatchJobTracker, Long> {
  @Query(
    """
    select t from OpenAiBatchJobTracker t
    where t.status in :statuses
    order by t.updatedAt asc
    """,
  )
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
  fun findAndLockByStatusIn(statuses: List<OpenAiBatchTrackerStatus>): List<OpenAiBatchJobTracker>

  fun findByChunkExecutionId(chunkExecutionId: Long): OpenAiBatchJobTracker?

  @Query(
    """
    select count(t) from OpenAiBatchJobTracker t
    where t.status in :statuses
    and t.batchJob.project.organizationOwner.id = :organizationId
    """,
  )
  fun countByStatusInAndOrganizationId(
    statuses: List<OpenAiBatchTrackerStatus>,
    organizationId: Long,
  ): Long

  @Query(
    """
    select count(t) from OpenAiBatchJobTracker t
    where t.status in :statuses
    """,
  )
  fun countByStatusIn(statuses: List<OpenAiBatchTrackerStatus>): Long
}
