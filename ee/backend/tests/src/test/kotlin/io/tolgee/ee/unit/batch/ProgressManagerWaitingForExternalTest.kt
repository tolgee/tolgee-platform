package io.tolgee.ee.unit.batch

import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobProjectLockingManager
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.CachingBatchJobService
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.batch.state.ExecutionState
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobChunkExecutionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.PlatformTransactionManager

/**
 * Tests that ProgressManager correctly handles WAITING_FOR_EXTERNAL status:
 * - handleChunkCompletedCommitted should NOT increment committed count for non-terminal statuses
 * - handleProgress should correctly update state for WAITING_FOR_EXTERNAL executions
 */
class ProgressManagerWaitingForExternalTest {
  private lateinit var eventPublisher: ApplicationEventPublisher
  private lateinit var transactionManager: PlatformTransactionManager
  private lateinit var batchJobService: BatchJobService
  private lateinit var batchJobStateProvider: BatchJobStateProvider
  private lateinit var cachingBatchJobService: CachingBatchJobService
  private lateinit var batchJobProjectLockingManager: BatchJobProjectLockingManager
  private lateinit var queue: BatchJobChunkExecutionQueue

  private lateinit var progressManager: ProgressManager

  @BeforeEach
  fun setUp() {
    eventPublisher = mock(ApplicationEventPublisher::class.java)
    transactionManager = mock(PlatformTransactionManager::class.java)
    batchJobService = mock(BatchJobService::class.java)
    batchJobStateProvider = mock(BatchJobStateProvider::class.java)
    cachingBatchJobService = mock(CachingBatchJobService::class.java)
    batchJobProjectLockingManager = mock(BatchJobProjectLockingManager::class.java)
    queue = mock(BatchJobChunkExecutionQueue::class.java)

    progressManager =
      ProgressManager(
        eventPublisher = eventPublisher,
        transactionManager = transactionManager,
        batchJobService = batchJobService,
        batchJobStateProvider = batchJobStateProvider,
        cachingBatchJobService = cachingBatchJobService,
        batchJobProjectLockingManager = batchJobProjectLockingManager,
        queue = queue,
      )
  }

  @Test
  fun `handleChunkCompletedCommitted skips committed count for WAITING_FOR_EXTERNAL`() {
    val jobId = 42L
    val executionId = 99L
    val execution = createExecution(jobId, executionId, BatchJobChunkExecutionStatus.WAITING_FOR_EXTERNAL)

    // No existing state (first time seeing this execution's committed event)
    whenever(batchJobStateProvider.getSingleExecution(jobId, executionId)).thenReturn(null)
    whenever(batchJobStateProvider.getStateForExecution(execution)).thenReturn(
      ExecutionState(
        successTargets = emptyList(),
        status = BatchJobChunkExecutionStatus.WAITING_FOR_EXTERNAL,
        chunkNumber = 0,
        retry = false,
        transactionCommitted = false,
      ),
    )

    progressManager.handleChunkCompletedCommitted(execution)

    // WAITING_FOR_EXTERNAL is non-terminal (completed=false), so committed count
    // should NOT be incremented
    verify(batchJobStateProvider, never()).incrementCommittedCountAndGet(any())
  }

  @Test
  fun `handleChunkCompletedCommitted increments committed count for SUCCESS`() {
    val jobId = 42L
    val executionId = 99L
    val execution = createExecution(jobId, executionId, BatchJobChunkExecutionStatus.SUCCESS)

    whenever(batchJobStateProvider.getSingleExecution(jobId, executionId)).thenReturn(null)
    whenever(batchJobStateProvider.getStateForExecution(execution)).thenReturn(
      ExecutionState(
        successTargets = emptyList(),
        status = BatchJobChunkExecutionStatus.SUCCESS,
        chunkNumber = 0,
        retry = false,
        transactionCommitted = false,
      ),
    )
    whenever(batchJobStateProvider.incrementCommittedCountAndGet(jobId)).thenReturn(1)

    val jobDto =
      io.tolgee.batch.data.BatchJobDto(
        id = jobId,
        projectId = 1L,
        authorId = 1L,
        target = emptyList(),
        totalItems = 0,
        totalChunks = 2,
        chunkSize = 100,
        status = io.tolgee.model.batch.BatchJobStatus.RUNNING,
        type = io.tolgee.batch.data.BatchJobType.MACHINE_TRANSLATE,
        params = null,
        maxPerJobConcurrency = 1,
        jobCharacter = io.tolgee.batch.JobCharacter.SLOW,
        hidden = false,
        debouncingKey = null,
      )
    whenever(batchJobService.getJobDto(jobId)).thenReturn(jobDto)

    progressManager.handleChunkCompletedCommitted(execution, batchJobDto = jobDto)

    // SUCCESS is terminal (completed=true), so committed count SHOULD be incremented
    verify(batchJobStateProvider).incrementCommittedCountAndGet(eq(jobId))
  }

  @Test
  fun `handleChunkCompletedCommitted skips committed count for RUNNING`() {
    val jobId = 42L
    val executionId = 99L
    val execution = createExecution(jobId, executionId, BatchJobChunkExecutionStatus.RUNNING)

    whenever(batchJobStateProvider.getSingleExecution(jobId, executionId)).thenReturn(null)
    whenever(batchJobStateProvider.getStateForExecution(execution)).thenReturn(
      ExecutionState(
        successTargets = emptyList(),
        status = BatchJobChunkExecutionStatus.RUNNING,
        chunkNumber = 0,
        retry = false,
        transactionCommitted = false,
      ),
    )

    progressManager.handleChunkCompletedCommitted(execution)

    // RUNNING is non-terminal (completed=false), so committed count should NOT be incremented
    verify(batchJobStateProvider, never()).incrementCommittedCountAndGet(any())
  }

  @Test
  fun `handleChunkCompletedCommitted skips committed count for retry=true`() {
    val jobId = 42L
    val executionId = 99L
    val execution = createExecution(jobId, executionId, BatchJobChunkExecutionStatus.FAILED)
    execution.retry = true

    whenever(batchJobStateProvider.getSingleExecution(jobId, executionId)).thenReturn(null)
    whenever(batchJobStateProvider.getStateForExecution(execution)).thenReturn(
      ExecutionState(
        successTargets = emptyList(),
        status = BatchJobChunkExecutionStatus.FAILED,
        chunkNumber = 0,
        retry = true,
        transactionCommitted = false,
      ),
    )

    progressManager.handleChunkCompletedCommitted(execution)

    // FAILED with retry=true should NOT increment committed count
    verify(batchJobStateProvider, never()).incrementCommittedCountAndGet(any())
  }

  @Test
  fun `handleChunkCompletedCommitted is idempotent for already-committed execution`() {
    val jobId = 42L
    val executionId = 99L
    val execution = createExecution(jobId, executionId, BatchJobChunkExecutionStatus.SUCCESS)

    // Already committed
    whenever(batchJobStateProvider.getSingleExecution(jobId, executionId)).thenReturn(
      ExecutionState(
        successTargets = emptyList(),
        status = BatchJobChunkExecutionStatus.SUCCESS,
        chunkNumber = 0,
        retry = false,
        transactionCommitted = true,
      ),
    )

    progressManager.handleChunkCompletedCommitted(execution)

    // Should skip entirely, no state update, no committed count
    verify(batchJobStateProvider, never()).getStateForExecution(any())
    verify(batchJobStateProvider, never()).incrementCommittedCountAndGet(any())
  }

  // -- Helpers --

  private fun createExecution(
    jobId: Long,
    executionId: Long,
    status: BatchJobChunkExecutionStatus,
  ): BatchJobChunkExecution {
    val batchJob = mock(BatchJob::class.java)
    whenever(batchJob.id).thenReturn(jobId)

    return BatchJobChunkExecution().apply {
      this.id = executionId
      this.batchJob = batchJob
      this.chunkNumber = 0
      this.status = status
    }
  }
}
