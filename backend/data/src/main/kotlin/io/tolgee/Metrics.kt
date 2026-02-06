package io.tolgee

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

@Component
class Metrics(
  private val meterRegistry: MeterRegistry,
) {
  fun registerJobQueue(queue: ConcurrentLinkedQueue<*>) {
    Gauge
      .builder("tolgee.batch.job.execution.queue.size", queue) { it.size.toDouble() }
      .description("Size of the queue of batch job executions")
      .register(meterRegistry)
  }

  val batchJobManagementItemAlreadyQueuedCounter: Counter by lazy {
    Counter
      .builder("tolgee.batch.job.execution.queue.already_queued")
      .description("Number of executions that were already queued, but retrieved again from db")
      .register(meterRegistry)
  }

  val batchJobManagementItemAlreadyLockedCounter: Counter by lazy {
    Counter
      .builder("tolgee.batch.job.execution.queue.already_locked")
      .description(
        "Number of executions that were already locked (by scheduled db retrieved or other pod) and are preprocessed redundantly",
      ).register(meterRegistry)
  }

  val batchJobManagementFailureWithRetryCounter: Counter by lazy {
    Counter
      .builder("tolgee.batch.job.execution.management.failure.retried")
      .description("Total number of failures when trying to store data about batch job execution (retried)")
      .register(meterRegistry)
  }

  val batchJobManagementTotalFailureFailedCounter: Counter by lazy {
    Counter
      .builder("tolgee.batch.job.execution.management.failure.failed")
      .description("Total number of failures when trying to store data about batch job execution (execution failed)")
      .register(meterRegistry)
  }

  val bigMetaStoringTimer: Timer by lazy {
    Timer
      .builder("tolgee.big_meta.storing.timer")
      .description("Time spent storing big meta data (sync)")
      .register(meterRegistry)
  }

  val bigMetaStoringAsyncTimer: Timer by lazy {
    Timer
      .builder("tolgee.big_meta.storing-async.timer")
      .description("Time spent storing big meta data (async)")
      .register(meterRegistry)
  }

  val bigMetaDeletingAsyncTimer: Timer by lazy {
    Timer
      .builder("tolgee.big_meta.deleting-async.timer")
      .description("Time spent deleting big meta data (async)")
      .register(meterRegistry)
  }

  val bigMetaNewDistancesComputeTimer: Timer by lazy {
    Timer
      .builder("tolgee.big_meta.new_distances.compute.timer")
      .description("Time spent computing new distances for big meta data")
      .register(meterRegistry)
  }

  // Batch API metrics

  val batchApiJobsSubmittedCounter: Counter by lazy {
    Counter
      .builder("tolgee.batch_api.jobs.submitted")
      .description("Total number of batch API jobs submitted to OpenAI")
      .register(meterRegistry)
  }

  fun batchApiJobsCompletedCounter(status: String): Counter =
    Counter
      .builder("tolgee.batch_api.jobs.completed")
      .tag("status", status)
      .description("Total number of batch API jobs completed, by status")
      .register(meterRegistry)

  fun batchApiTranslationsProcessedCounter(status: String): Counter =
    Counter
      .builder("tolgee.batch_api.translations.processed")
      .tag("status", status)
      .description("Total number of translations processed via batch API, by status")
      .register(meterRegistry)

  fun batchApiFallbackTriggeredCounter(reason: String): Counter =
    Counter
      .builder("tolgee.batch_api.fallback.triggered")
      .tag("reason", reason)
      .description("Total number of batch API fallbacks triggered, by reason")
      .register(meterRegistry)

  val batchApiJobDurationSeconds: Timer by lazy {
    Timer
      .builder("tolgee.batch_api.job.duration")
      .description("Duration of batch API jobs from submission to completion")
      .register(meterRegistry)
  }

  private val batchApiActiveJobsCount = AtomicLong(0)

  val batchApiActiveJobsGauge: Gauge by lazy {
    Gauge
      .builder("tolgee.batch_api.active_jobs", batchApiActiveJobsCount) { it.toDouble() }
      .description("Number of currently active batch API jobs")
      .register(meterRegistry)
  }

  fun incrementBatchApiActiveJobs() {
    batchApiActiveJobsGauge // ensure registered
    batchApiActiveJobsCount.incrementAndGet()
  }

  fun decrementBatchApiActiveJobs() {
    batchApiActiveJobsGauge // ensure registered
    batchApiActiveJobsCount.decrementAndGet()
  }

  val batchApiPollDurationSeconds: Timer by lazy {
    Timer
      .builder("tolgee.batch_api.poll.duration")
      .description("Duration of individual batch API poll cycles")
      .register(meterRegistry)
  }
}
