package io.tolgee.batch.timing

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Aspect
@Component
@ConditionalOnProperty(
  value = ["tolgee.internal.controller-enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
class BatchJobTimingAspect(
  private val timer: BatchJobOperationTimer,
) {
  // ============ Core pipeline ============

  @Around("execution(* io.tolgee.batch.BatchJobActionService.handleItem(..))")
  fun measureHandleItem(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("BatchJobActionService.handleItem") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.BatchJobActionService.getExecutionIfCanAcquireLockInDb(..))")
  fun measureDbLockAcquisition(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("DB_LOCK_ACQUIRE") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.BatchJobActionService.publishRemoveConsuming(..))")
  fun measureRedisPublish(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("REDIS_PUBLISH_REMOVE") {
      joinPoint.proceed()
    }
  }

  // ============ Queue operations ============

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

  @Around("execution(* io.tolgee.batch.BatchJobChunkExecutionQueue.pollRoundRobin(..))")
  fun measureQueuePollRoundRobin(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("QUEUE_POLL_ROUND_ROBIN") {
      joinPoint.proceed()
    }
  }

  // ============ Chunk processing ============

  @Around("execution(* io.tolgee.batch.ChunkProcessingUtil.processChunk(..))")
  fun measureProcessChunk(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("CHUNK_PROCESS") {
      joinPoint.proceed()
    }
  }

  // ============ State management ============

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.get(..))")
  fun measureStateGet(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getInitialState(..))")
  fun measureGetInitialState(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INIT") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getSingleExecution(..))")
  fun measureGetSingleExecution(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_SINGLE_EXEC") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getStateForExecution(..))")
  fun measureGetStateForExecution(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_FOR_EXEC") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.updateSingleExecution(..))")
  fun measureUpdateSingleExecution(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_UPDATE_SINGLE_EXEC") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.addProgressCount(..))")
  fun measureAddProgressCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_ADD_PROGRESS") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementCompletedChunksCount(..))")
  fun measureIncrementCompletedChunks(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_COMPLETED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementFailedCount(..))")
  fun measureIncrementFailed(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_FAILED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementCancelledCount(..))")
  fun measureIncrementCancelled(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_CANCELLED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getCompletedChunksCount(..))")
  fun measureGetCompletedChunksCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_COMPLETED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getProgressCount(..))")
  fun measureGetProgressCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_PROGRESS") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getCancelledCount(..))")
  fun measureGetCancelledCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_CANCELLED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.getFailedCount(..))")
  fun measureGetFailedCount(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_GET_FAILED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.state.BatchJobStateProvider.incrementCommittedCountAndGet(..))")
  fun measureIncrementCommittedCountAndGet(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("STATE_INC_COMMITTED") {
      joinPoint.proceed()
    }
  }

  // ============ Locking ============

  @Around("execution(* io.tolgee.component.LockingProvider.withLocking(..))")
  fun measureLocking(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("LOCKING_PROVIDER") {
      joinPoint.proceed()
    }
  }

  // ============ Progress management ============

  @Around("execution(* io.tolgee.batch.ProgressManager.handleProgress(..))")
  fun measureHandleProgress(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("PROGRESS_HANDLE") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.ProgressManager.handleChunkCompletedCommitted(..))")
  fun measureHandleChunkCompleted(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("CHUNK_COMPLETED_COMMITTED") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.ProgressManager.handleJobRunning(..))")
  fun measureHandleJobRunning(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("JOB_RUNNING_HANDLE") {
      joinPoint.proceed()
    }
  }

  @Around("execution(* io.tolgee.batch.ProgressManager.handleJobStatus(..))")
  fun measureHandleJobStatus(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("PROGRESS_HANDLE_JOB_STATUS") {
      joinPoint.proceed()
    }
  }

  // ============ Caching service ============

  @Around("execution(* io.tolgee.batch.CachingBatchJobService.*(..))")
  fun measureCachingService(joinPoint: ProceedingJoinPoint): Any? {
    val methodName = joinPoint.signature.name
    return timer.measure("CACHING_SERVICE.$methodName") {
      joinPoint.proceed()
    }
  }

  // ============ BatchJobService ============

  @Around("execution(* io.tolgee.batch.BatchJobService.getJobDto(..))")
  fun measureGetJobDto(joinPoint: ProceedingJoinPoint): Any? {
    return timer.measure("GET_JOB_DTO") {
      joinPoint.proceed()
    }
  }

  // ============ Event publishing (batch job events only) ============

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
        else -> return joinPoint.proceed()
      }
    return timer.measure(eventType) {
      joinPoint.proceed()
    }
  }
}
