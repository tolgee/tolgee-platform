package io.tolgee.batch

import io.tolgee.component.AtomicLongProvider
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.util.TolgeeAtomicLong
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigInteger
import javax.persistence.EntityManager

@Component
class ProgressManager(
  private val atomicLongProvider: AtomicLongProvider,
  private val entityManager: EntityManager,
  private val eventPublisher: ApplicationEventPublisher
) {
  private fun getProgressAtomicLong(jobId: Long): TolgeeAtomicLong {
    return atomicLongProvider.get("batch_job_progress_$jobId") {
      entityManager.createNativeQuery(
        """
        select sum(jsonb_array_length(success_targets)) 
        from batch_job_chunk_execution 
        where batch_job_id = :id
        """
      ).setParameter("id", jobId).singleResult?.let { (it as BigInteger).toLong() } ?: 0
    }
  }

  fun handleProgress(execution: BatchJobChunkExecution) {
    val job = execution.batchJob
    val totalTargets = job.totalItems
    val progress = getProgressAtomicLong(job.id).addAndGet(execution.successTargets.size.toLong())
    eventPublisher.publishEvent(OnBatchOperationProgress(job, progress, totalTargets.toLong()))
    if (totalTargets <= progress) {
      job.status = BatchJobStatus.SUCCESS
      entityManager.persist(job)
    }
  }
}
