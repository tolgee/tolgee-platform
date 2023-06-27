package io.tolgee.batch

import io.tolgee.component.LockingProvider
import io.tolgee.component.atomicLong.AtomicLongProvider
import io.tolgee.util.Logging
import io.tolgee.util.TolgeeAtomicLong
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import java.math.BigInteger
import javax.persistence.EntityManager

@Component
class AtomicProgressState(
  private val atomicLongProvider: AtomicLongProvider,
  private val entityManager: EntityManager,
  private val lockingProvider: LockingProvider
) : Logging {
  fun <T> withAtomicState(
    jobId: Long,
    currentExecutionId: Long? = null,
    fn: (totalProgress: TolgeeAtomicLong, chunkProgress: TolgeeAtomicLong) -> T
  ): T {
    val lock = lockingProvider.getLock("batch_job_atomic_state_$jobId")
    lock.lock()
    try {
      return fn(
        getProgressAtomicLong(jobId, currentExecutionId),
        getCompletedChunksAtomicLong(jobId, currentExecutionId)
      )
    } finally {
      lock.unlock()
    }
  }

  fun getAtomicState(jobId: Long): Pair<Long, Long> {
    return this.withAtomicState(jobId) { progress, chunks ->
      progress.get() to chunks.get()
    }
  }

  private fun getProgressAtomicLong(jobId: Long, currentExecutionId: Long? = null): TolgeeAtomicLong {
    return atomicLongProvider.get("batch_job_progress_$jobId") {
      getInitialProgress(jobId, currentExecutionId)
    }
  }

  private fun getCompletedChunksAtomicLong(jobId: Long, currentExecutionId: Long? = null): TolgeeAtomicLong {
    return atomicLongProvider.get("batch_job_completed_chunks_$jobId") {
      val initial = getInitialCompletedChunks(jobId, currentExecutionId)
      logger.debug("Initial completed chunks: $initial")
      initial
    }
  }

  fun getCompletedChunksCommittedAtomicLong(jobId: Long, currentExecutionId: Long? = null): TolgeeAtomicLong {
    return atomicLongProvider.get("batch_job_completed_chunks_committed_$jobId") {
      val initial = getInitialCompletedChunks(jobId, currentExecutionId)
      logger.debug("Initial completed chunks: $initial")
      initial
    }
  }

  private fun getInitialCompletedChunks(jobId: Long, currentExecutionId: Long?): Long {
    return entityManager.createQuery(
      """
      select count(id) from BatchJobChunkExecution bjce
      where bjce.batchJob.id = :jobId and bjce.retry = false and bjce.status != 'PENDING' and bjce.id <> :currentId
      """.trimIndent()
    )
      .setParameter("jobId", jobId)
      .setParameter("currentId", currentExecutionId)
      .singleResult?.let { it as Long } ?: 0
  }

  private fun getInitialProgress(jobId: Long, currentExecutionId: Long?): Long {
    return entityManager.createNativeQuery(
      """
            select sum(jsonb_array_length(success_targets)) 
            from batch_job_chunk_execution 
            where batch_job_id = :jobId and id <> :currentId
            """
    )
      .setParameter("jobId", jobId)
      .setParameter("currentId", currentExecutionId ?: 0)
      .singleResult?.let { (it as BigInteger).toLong() } ?: 0
  }
}
