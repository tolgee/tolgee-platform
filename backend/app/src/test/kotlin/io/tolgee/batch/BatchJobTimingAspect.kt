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

  // Measure BatchJobStateProvider.updateState (distributed lock + state update)
  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.updateState(..))")
  fun measureStateUpdate(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_UPDATE") {
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
}
