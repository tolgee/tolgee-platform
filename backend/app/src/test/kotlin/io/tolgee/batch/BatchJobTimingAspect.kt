package io.tolgee.batch

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

/**
 * AOP Aspect for timing batch job operations.
 * Note: Not annotated with @Component to avoid auto-scanning issues.
 * Must be explicitly registered as a bean in test configuration.
 */
@Aspect
class BatchJobTimingAspect(
  private val timer: BatchJobOperationTimer,
) {
  // Measure BatchJobActionService.handleItem
  @Around("execution(* io.tolgee.batch.BatchJobActionService.handleItem(..))")
  fun measureHandleItem(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("BatchJobActionService.handleItem") {
      joinPoint.proceed()
    }
  }

  // Measure DB lock acquisition
  @Around("execution(* io.tolgee.batch.BatchJobActionService.getExecutionIfCanAcquireLockInDb(..))")
  fun measureDbLockAcquisition(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("DB_LOCK_ACQUIRE") {
      joinPoint.proceed()
    }
  }

  // Measure BatchJobStateProvider.get (state retrieval)
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.get(..))")
  fun measureStateGet(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET") {
      joinPoint.proceed()
    }
  }

  // Measure BatchJobStateProvider.getInitialState
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getInitialState(..))")
  fun measureGetInitialState(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INIT") {
      joinPoint.proceed()
    }
  }

  // Measure LockingProvider.withLocking
  @Around("execution(* io.tolgee.component.LockingProvider.withLocking(..))")
  fun measureLocking(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("LOCKING_PROVIDER") {
      joinPoint.proceed()
    }
  }

  // Measure ProgressManager.handleProgress
  @Around("execution(* io.tolgee.batch.ProgressManager.handleProgress(..))")
  fun measureHandleProgress(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("PROGRESS_HANDLE") {
      joinPoint.proceed()
    }
  }

  // Measure ProgressManager.handleChunkCompletedCommitted
  @Around("execution(* io.tolgee.batch.ProgressManager.handleChunkCompletedCommitted(..))")
  fun measureHandleChunkCompleted(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("CHUNK_COMPLETED_COMMITTED") {
      joinPoint.proceed()
    }
  }

  // Measure ProgressManager.handleJobRunning
  @Around("execution(* io.tolgee.batch.ProgressManager.handleJobRunning(..))")
  fun measureHandleJobRunning(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("JOB_RUNNING_HANDLE") {
      joinPoint.proceed()
    }
  }

  // Measure ChunkProcessingUtil.processChunk
  @Around("execution(* io.tolgee.batch.ChunkProcessingUtil.processChunk(..))")
  fun measureProcessChunk(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("CHUNK_PROCESS") {
      joinPoint.proceed()
    }
  }

  // Measure BatchJobService.getJobDto
  @Around("execution(* io.tolgee.batch.BatchJobService.getJobDto(..))")
  fun measureGetJobDto(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("GET_JOB_DTO") {
      joinPoint.proceed()
    }
  }

  // Measure CachingBatchJobService operations
  @Around("execution(* io.tolgee.batch.CachingBatchJobService.*(..))")
  fun measureCachingService(joinPoint: ProceedingJoinPoint): Any? {
    val methodName = joinPoint.signature.name
    return timer.measure("CACHING_SERVICE.$methodName") {
      joinPoint.proceed()
    }
  }

  // Measure queue operations
  @Around("execution(* io.tolgee.batch.BatchJobChunkExecutionQueue.addItemsToQueue(..))")
  fun measureQueueAdd(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("QUEUE_ADD") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.BatchJobChunkExecutionQueue.poll(..))")
  fun measureQueuePoll(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("QUEUE_POLL") {
      joinPoint.proceed()
    }
  }

  // Measure Redis publish
  @Around("execution(* io.tolgee.batch.BatchJobActionService.publishRemoveConsuming(..))")
  fun measureRedisPublish(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("REDIS_PUBLISH_REMOVE") {
      joinPoint.proceed()
    }
  }

  // ============ BatchJobStateProvider operations (inside handleProgress) ============

  // Measure getSingleExecution
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getSingleExecution(..))")
  fun measureGetSingleExecution(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_SINGLE_EXEC") {
      joinPoint.proceed()
    }
  }

  // Measure getStateForExecution
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getStateForExecution(..))")
  fun measureGetStateForExecution(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_FOR_EXEC") {
      joinPoint.proceed()
    }
  }

  // Measure updateSingleExecution
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.updateSingleExecution(..))")
  fun measureUpdateSingleExecution(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_UPDATE_SINGLE_EXEC") {
      joinPoint.proceed()
    }
  }

  // Measure addProgressCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.addProgressCount(..))")
  fun measureAddProgressCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_ADD_PROGRESS") {
      joinPoint.proceed()
    }
  }

  // Measure incrementCompletedChunksCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementCompletedChunksCount(..))")
  fun measureIncrementCompletedChunks(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_COMPLETED") {
      joinPoint.proceed()
    }
  }

  // Measure incrementFailedCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementFailedCount(..))")
  fun measureIncrementFailed(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_FAILED") {
      joinPoint.proceed()
    }
  }

  // Measure incrementCancelledCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementCancelledCount(..))")
  fun measureIncrementCancelled(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_CANCELLED") {
      joinPoint.proceed()
    }
  }

  // Measure getCompletedChunksCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getCompletedChunksCount(..))")
  fun measureGetCompletedChunksCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_COMPLETED") {
      joinPoint.proceed()
    }
  }

  // Measure getProgressCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getProgressCount(..))")
  fun measureGetProgressCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_PROGRESS") {
      joinPoint.proceed()
    }
  }

  // Measure getCancelledCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getCancelledCount(..))")
  fun measureGetCancelledCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_CANCELLED") {
      joinPoint.proceed()
    }
  }

  // Measure getFailedCount
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getFailedCount(..))")
  fun measureGetFailedCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_FAILED") {
      joinPoint.proceed()
    }
  }

  // Measure incrementCommittedCountAndGet
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementCommittedCountAndGet(..))")
  fun measureIncrementCommittedCountAndGet(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_COMMITTED") {
      joinPoint.proceed()
    }
  }

  // ============ Event publishing ============

  // Measure all batch job event publishing
  @Around("execution(* org.springframework.context.ApplicationEventPublisher.publishEvent(..)) && args(event)")
  fun measureEventPublish(
    joinPoint: ProceedingJoinPoint,
    event: Any,
  ): Any? {
    val eventType =
      when {
        event.javaClass.name.contains("OnBatchJobProgress") -> "EVENT_PROGRESS"
        event.javaClass.name.contains("OnBatchJobSucceeded") -> "EVENT_SUCCEEDED"
        event.javaClass.name.contains("OnBatchJobFailed") -> "EVENT_FAILED"
        event.javaClass.name.contains("OnBatchJobCancelled") -> "EVENT_CANCELLED"
        event.javaClass.name.contains("OnBatchJobStatusUpdated") -> "EVENT_STATUS_UPDATED"
        event.javaClass.name.contains("BatchJob") -> "EVENT_OTHER_BATCH"
        else -> return joinPoint.proceed() // Don't time non-batch events
      }
    return timer.measure(eventType) {
      joinPoint.proceed()
    }
  }

  // ============ ProgressManager internal operations ============

  // Measure handleJobStatus
  @Around("execution(* io.tolgee.batch.ProgressManager.handleJobStatus(..))")
  fun measureHandleJobStatus(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("PROGRESS_HANDLE_JOB_STATUS") {
      joinPoint.proceed()
    }
  }
}
