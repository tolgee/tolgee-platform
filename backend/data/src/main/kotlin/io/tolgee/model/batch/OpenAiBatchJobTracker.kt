package io.tolgee.model.batch

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.model.StandardAuditModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Type

/**
 * Tracks the lifecycle of a single OpenAI Batch API job.
 *
 * Each tracker corresponds to one chunk execution submitted to OpenAI.
 * It stores the external batch/file IDs, progress counters, parsed results
 * (JSONB), and the current status in the [OpenAiBatchTrackerStatus] state machine.
 *
 * Optimistic locking via [version] prevents concurrent poll cycles from
 * producing conflicting updates.
 *
 * See `docs/batch-api/README.md` for the full state machine reference.
 */
@Entity
@Table(
  name = "openai_batch_job_tracker",
  indexes = [
    Index(columnList = "batch_job_id"),
    Index(columnList = "status"),
    Index(columnList = "openai_batch_id"),
  ],
)
class OpenAiBatchJobTracker : StandardAuditModel() {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_job_id")
  lateinit var batchJob: BatchJob

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chunk_execution_id")
  lateinit var chunkExecution: BatchJobChunkExecution

  @Column(name = "openai_batch_id", nullable = false, unique = true)
  lateinit var openAiBatchId: String

  @Column(name = "openai_input_file_id", nullable = false)
  lateinit var openAiInputFileId: String

  @Column(name = "openai_output_file_id")
  var openAiOutputFileId: String? = null

  @Column(name = "openai_error_file_id")
  var openAiErrorFileId: String? = null

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: OpenAiBatchTrackerStatus = OpenAiBatchTrackerStatus.SUBMITTED

  @Column(name = "openai_status")
  var openAiStatus: String? = null

  @Column(name = "total_requests")
  var totalRequests: Int = 0

  @Column(name = "completed_requests")
  var completedRequests: Int = 0

  @Column(name = "failed_requests")
  var failedRequests: Int = 0

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var results: List<OpenAiBatchResult>? = null

  @Column(columnDefinition = "text")
  var errorMessage: String? = null

  @Column(name = "provider_id")
  var providerId: Long? = null

  @Version
  var version: Long = 0
}
