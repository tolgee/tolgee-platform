package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.cleaning.ScheduledJobCleaner
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ScheduledJobCleanerOrphanLocksTest : AbstractSpringTest() {
  @Autowired
  lateinit var scheduledJobCleaner: ScheduledJobCleaner

  @Autowired
  lateinit var lockingManager: BatchJobProjectLockingManager

  lateinit var testData: BaseTestData

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `clears orphan project lock pointing at a non-existent job`() {
    val projectId = testData.project.id
    val deletedJobId = 9_876_543_210L
    lockingManager.getMap()[projectId] = deletedJobId

    scheduledJobCleaner.cleanup()

    lockingManager.getLockedForProject(projectId).assert.isEqualTo(0L)
  }

  @Test
  fun `keeps project lock pointing at an existing job`() {
    val projectId = testData.project.id
    val liveJob = createJob(BatchJobStatus.PENDING)
    lockingManager.getMap()[projectId] = liveJob.id

    scheduledJobCleaner.cleanup()

    lockingManager.getLockedForProject(projectId).assert.isEqualTo(liveJob.id)
  }

  @Test
  fun `leaves freed lock slots (value zero) alone`() {
    val projectId = testData.project.id
    lockingManager.getMap()[projectId] = 0L

    scheduledJobCleaner.cleanup()

    lockingManager.getLockedForProject(projectId).assert.isEqualTo(0L)
  }

  private fun createJob(status: BatchJobStatus): BatchJob =
    executeInNewTransaction(platformTransactionManager) {
      val job =
        BatchJob().apply {
          this.status = status
          this.project = testData.project
          this.totalChunks = 0
        }
      entityManager.persist(job)
      entityManager.flush()
      job
    }
}
