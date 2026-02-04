package io.tolgee

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue

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

  // ==========================================================================
  // Batch Job Performance Metrics
  // ==========================================================================

  /**
   * Distribution of items processed per job.
   */
  val batchJobItemsProcessed: DistributionSummary by lazy {
    DistributionSummary
      .builder("tolgee.batch.job.items_processed")
      .description("Number of items processed per batch job")
      .publishPercentileHistogram()
      .register(meterRegistry)
  }

  /**
   * Records job completion with duration and tags.
   *
   * Note on cardinality: project_id and organization_id are included intentionally to enable
   * per-customer performance analysis during debugging. In production with many projects,
   * consider using Prometheus recording rules to pre-aggregate if cardinality becomes an issue.
   * organization_name is excluded to limit cardinality and avoid exposing customer data in metrics.
   */
  fun recordJobCompleted(
    jobType: String,
    status: String,
    projectId: Long?,
    organizationId: Long?,
    durationMs: Long,
  ) {
    val projectTag = projectId?.toString() ?: "unknown"
    val orgIdTag = organizationId?.toString() ?: "unknown"

    // Create a tagged timer for this specific combination
    Timer
      .builder("tolgee.batch.job.duration")
      .description("Total duration of batch jobs from creation to completion")
      .publishPercentileHistogram()
      .tag("job_type", jobType)
      .tag("status", status)
      .tag("project_id", projectTag)
      .tag("organization_id", orgIdTag)
      .register(meterRegistry)
      .record(Duration.ofMillis(durationMs.coerceAtLeast(0)))

    Counter
      .builder("tolgee.batch.job.completed")
      .tag("job_type", jobType)
      .tag("status", status)
      .tag("project_id", projectTag)
      .tag("organization_id", orgIdTag)
      .description("Total number of completed batch jobs")
      .register(meterRegistry)
      .increment()
  }

  /**
   * Records job started with tags.
   * Note: organization_name intentionally excluded to limit cardinality and avoid exposing customer data in metrics.
   */
  fun recordJobStarted(
    jobType: String,
    jobCharacter: String,
    projectId: Long?,
    organizationId: Long?,
  ) {
    Counter
      .builder("tolgee.batch.job.started")
      .tag("job_type", jobType)
      .tag("job_character", jobCharacter)
      .tag("project_id", projectId?.toString() ?: "unknown")
      .tag("organization_id", organizationId?.toString() ?: "unknown")
      .description("Total number of started batch jobs")
      .register(meterRegistry)
      .increment()
  }

  /**
   * Records queue wait time with tags.
   */
  fun recordQueueWaitTime(
    jobType: String,
    jobCharacter: String,
    waitTimeMs: Long,
  ) {
    Timer
      .builder("tolgee.batch.job.queue_wait")
      .description("Time jobs wait before first chunk executes")
      .publishPercentileHistogram()
      .tag("job_type", jobType)
      .tag("job_character", jobCharacter)
      .register(meterRegistry)
      .record(Duration.ofMillis(waitTimeMs.coerceAtLeast(0)))
  }

  /**
   * Records chunk execution time with tags.
   */
  fun recordChunkExecutionTime(
    jobType: String,
    status: String,
    executionTimeMs: Long,
  ) {
    Timer
      .builder("tolgee.batch.chunk.execution")
      .description("Time to execute a single batch job chunk")
      .publishPercentileHistogram()
      .tag("job_type", jobType)
      .tag("status", status)
      .register(meterRegistry)
      .record(Duration.ofMillis(executionTimeMs.coerceAtLeast(0)))
  }

  // Branch operations - Priority 1 (Must Have)
  val branchCreateTimer: Timer by lazy {
    Timer
      .builder("tolgee.branch.create.timer")
      .description("Time spent creating branches")
      .register(meterRegistry)
  }

  val branchDeleteTimer: Timer by lazy {
    Timer
      .builder("tolgee.branch.delete.timer")
      .description("Time spent deleting branches (includes cleanup)")
      .register(meterRegistry)
  }

  // Merge operations
  val branchMergePreviewTimer: Timer by lazy {
    Timer
      .builder("tolgee.branch.merge.preview.timer")
      .description("Time spent generating merge preview")
      .register(meterRegistry)
  }

  val branchMergeApplyTimer: Timer by lazy {
    Timer
      .builder("tolgee.branch.merge.apply.timer")
      .description("Time spent applying merge")
      .register(meterRegistry)
  }

  fun branchMergeConflictCounter(resolution: String): Counter =
    meterRegistry.counter("tolgee.branch.merge.conflicts.total", "resolution", resolution)

  // Branch operations - Priority 2 (Should Have)
  val branchCleanupBatchesCounter: Counter by lazy {
    Counter
      .builder("tolgee.branch.cleanup.batches.total")
      .description("Number of cleanup batches processed")
      .register(meterRegistry)
  }

  val languageStatsRefreshTimer: Timer by lazy {
    Timer
      .builder("tolgee.language_stats.refresh.timer")
      .description("Time spent refreshing language stats")
      .register(meterRegistry)
  }
}
