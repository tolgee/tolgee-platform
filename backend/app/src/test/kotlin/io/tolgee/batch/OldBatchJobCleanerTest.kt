package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.cleaning.OldBatchJobCleaner
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.assert
import io.tolgee.util.addDays
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.Date

@SpringBootTest(
  properties = ["tolgee.batch.old-job-cleanup-enabled=true"],
)
class OldBatchJobCleanerTest : AbstractSpringTest() {
  @Autowired
  lateinit var oldBatchJobCleaner: OldBatchJobCleaner

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    clearForcedDate()
  }

  @Test
  fun `deletes old SUCCESS jobs older than retention period`() {
    val oldDate = Date().addDays(-5)
    val oldJob = createJobWithStatus(BatchJobStatus.SUCCESS, oldDate)

    oldBatchJobCleaner.cleanup()

    assertJobDeleted(oldJob.id)
  }

  @Test
  fun `deletes old CANCELLED jobs older than retention period`() {
    val oldDate = Date().addDays(-5)
    val oldJob = createJobWithStatus(BatchJobStatus.CANCELLED, oldDate)

    oldBatchJobCleaner.cleanup()

    assertJobDeleted(oldJob.id)
  }

  @Test
  fun `keeps recent SUCCESS jobs within retention period`() {
    val recentDate = Date().addDays(-1)
    val recentJob = createJobWithStatus(BatchJobStatus.SUCCESS, recentDate)

    oldBatchJobCleaner.cleanup()

    assertJobExists(recentJob.id)
  }

  @Test
  fun `keeps recent CANCELLED jobs within retention period`() {
    val recentDate = Date().addDays(-1)
    val recentJob = createJobWithStatus(BatchJobStatus.CANCELLED, recentDate)

    oldBatchJobCleaner.cleanup()

    assertJobExists(recentJob.id)
  }

  @Test
  fun `deletes old FAILED jobs older than retention period`() {
    val oldDate = Date().addDays(-35)
    val oldJob = createJobWithStatus(BatchJobStatus.FAILED, oldDate)

    oldBatchJobCleaner.cleanup()

    assertJobDeleted(oldJob.id)
  }

  @Test
  fun `keeps recent FAILED jobs within retention period`() {
    val recentDate = Date().addDays(-10)
    val recentJob = createJobWithStatus(BatchJobStatus.FAILED, recentDate)

    oldBatchJobCleaner.cleanup()

    assertJobExists(recentJob.id)
  }

  @Test
  fun `never deletes RUNNING jobs regardless of age`() {
    val oldDate = Date().addDays(-100)
    val runningJob = createJobWithStatus(BatchJobStatus.RUNNING, oldDate)

    oldBatchJobCleaner.cleanup()

    assertJobExists(runningJob.id)
  }

  @Test
  fun `never deletes PENDING jobs regardless of age`() {
    val oldDate = Date().addDays(-100)
    val pendingJob = createJobWithStatus(BatchJobStatus.PENDING, oldDate)

    oldBatchJobCleaner.cleanup()

    assertJobExists(pendingJob.id)
  }

  @Test
  fun `deletes chunk executions along with job`() {
    val oldDate = Date().addDays(-5)
    val job = createJobWithExecutions(BatchJobStatus.SUCCESS, oldDate)
    val executionIds = getExecutionIds(job.id)
    executionIds.assert.isNotEmpty

    oldBatchJobCleaner.cleanup()

    assertJobDeleted(job.id)
    executionIds.forEach { assertExecutionDeleted(it) }
  }

  @Test
  fun `handles multiple batches when many jobs need deletion`() {
    val oldDate = Date().addDays(-5)
    val jobs = (1..15).map { createJobWithStatus(BatchJobStatus.SUCCESS, oldDate) }

    oldBatchJobCleaner.cleanup()

    jobs.forEach { assertJobDeleted(it.id) }
  }

  @Test
  fun `cleanupCompletedJobs only deletes completed jobs`() {
    val oldDate = Date().addDays(-5)
    val successJob = createJobWithStatus(BatchJobStatus.SUCCESS, oldDate)
    val cancelledJob = createJobWithStatus(BatchJobStatus.CANCELLED, oldDate)
    val failedJob = createJobWithStatus(BatchJobStatus.FAILED, oldDate)
    val runningJob = createJobWithStatus(BatchJobStatus.RUNNING, oldDate)

    oldBatchJobCleaner.cleanupCompletedJobs()

    assertJobDeleted(successJob.id)
    assertJobDeleted(cancelledJob.id)
    assertJobExists(failedJob.id)
    assertJobExists(runningJob.id)
  }

  @Test
  fun `cleanupFailedJobs only deletes failed jobs`() {
    val oldDate = Date().addDays(-35)
    val successJob = createJobWithStatus(BatchJobStatus.SUCCESS, oldDate)
    val failedJob = createJobWithStatus(BatchJobStatus.FAILED, oldDate)
    val runningJob = createJobWithStatus(BatchJobStatus.RUNNING, oldDate)

    oldBatchJobCleaner.cleanupFailedJobs()

    assertJobExists(successJob.id)
    assertJobDeleted(failedJob.id)
    assertJobExists(runningJob.id)
  }

  private fun createJobWithStatus(
    status: BatchJobStatus,
    updatedAt: Date,
  ): BatchJob {
    return executeInNewTransaction(platformTransactionManager) {
      val job =
        BatchJob().apply {
          this.status = status
          this.project = testData.project
          this.totalChunks = 0
        }
      entityManager.persist(job)
      entityManager.flush()

      // Use native query to set updatedAt (bypassing @LastModifiedDate)
      entityManager
        .createNativeQuery("UPDATE tolgee_batch_job SET updated_at = :updatedAt WHERE id = :id")
        .setParameter("updatedAt", updatedAt)
        .setParameter("id", job.id)
        .executeUpdate()

      job
    }
  }

  private fun createJobWithExecutions(
    status: BatchJobStatus,
    updatedAt: Date,
  ): BatchJob {
    return executeInNewTransaction(platformTransactionManager) {
      val job =
        BatchJob().apply {
          this.status = status
          this.project = testData.project
          this.totalChunks = 3
        }
      entityManager.persist(job)

      (0..2).forEach { chunkNumber ->
        val execution =
          BatchJobChunkExecution().apply {
            this.batchJob = job
            this.status = BatchJobChunkExecutionStatus.SUCCESS
            this.chunkNumber = chunkNumber
          }
        entityManager.persist(execution)
      }

      entityManager.flush()

      // Use native query to set updatedAt (bypassing @LastModifiedDate)
      entityManager
        .createNativeQuery("UPDATE tolgee_batch_job SET updated_at = :updatedAt WHERE id = :id")
        .setParameter("updatedAt", updatedAt)
        .setParameter("id", job.id)
        .executeUpdate()

      job
    }
  }

  private fun getExecutionIds(jobId: Long): List<Long> {
    return executeInNewTransaction(platformTransactionManager) {
      @Suppress("UNCHECKED_CAST")
      entityManager
        .createQuery("SELECT e.id FROM BatchJobChunkExecution e WHERE e.batchJob.id = :jobId")
        .setParameter("jobId", jobId)
        .resultList as List<Long>
    }
  }

  private fun assertJobDeleted(jobId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      val result =
        entityManager
          .createQuery("SELECT COUNT(j) FROM BatchJob j WHERE j.id = :id")
          .setParameter("id", jobId)
          .singleResult as Long
      result.assert.isEqualTo(0L)
    }
  }

  private fun assertJobExists(jobId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      val result =
        entityManager
          .createQuery("SELECT COUNT(j) FROM BatchJob j WHERE j.id = :id")
          .setParameter("id", jobId)
          .singleResult as Long
      result.assert.isEqualTo(1L)
    }
  }

  private fun assertExecutionDeleted(executionId: Long) {
    executeInNewTransaction(platformTransactionManager) {
      val result =
        entityManager
          .createQuery("SELECT COUNT(e) FROM BatchJobChunkExecution e WHERE e.id = :id")
          .setParameter("id", executionId)
          .singleResult as Long
      result.assert.isEqualTo(0L)
    }
  }
}
