package io.tolgee.ee.unit.batch

import io.tolgee.component.CurrentDateProvider
import io.tolgee.ee.service.BatchApiError
import io.tolgee.ee.service.BatchStatusResult
import io.tolgee.ee.service.BatchSubmissionResult
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.ee.service.RequestCounts
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A full replacement for [OpenAiBatchApiService] in tests.
 *
 * Provides a configurable state machine (VALIDATING -> IN_PROGRESS -> FINALIZING -> COMPLETED,
 * also FAILED, EXPIRED, CANCELLED), assertion tracking, and manual helpers.
 */
@Primary
@Profile("test")
@Component
class FakeOpenAiBatchApiService(
  private val currentDateProvider: CurrentDateProvider,
) : OpenAiBatchApiService {
  // -- Configuration knobs --------------------------------------------------

  /** When true, batches complete immediately on creation (skip all intermediate states). */
  var instantCompletion: Boolean = false

  /** When > 0 and [instantCompletion] is false, batches complete after this delay (ms). */
  var completionDelayMs: Long = 0

  /** If non-null, the next [submitBatch] call will throw this exception. */
  var nextCreateError: Exception? = null

  /** If non-null, the next [pollBatchStatus] call will throw this exception. */
  var nextPollError: Exception? = null

  /** Number of items to report as failed in [RequestCounts]. */
  var failedItemCount: Int = 0

  /** When true, batches stay in VALIDATING and never advance. */
  var stuckInValidating: Boolean = false

  // -- Assertion tracking ---------------------------------------------------

  data class Submission(
    val apiKey: String,
    val apiUrl: String,
    val jsonlContent: ByteArray,
    val endpoint: String,
    val model: String,
    val metadata: Map<String, String>?,
    val batchId: String,
  )

  data class PollCall(
    val apiKey: String,
    val apiUrl: String,
    val batchId: String,
  )

  val submissions = mutableListOf<Submission>()
  val pollCalls = mutableListOf<PollCall>()
  val cancelCalls = mutableListOf<String>()
  val deletedFiles = mutableListOf<String>()

  // -- Internal state -------------------------------------------------------

  data class FakeBatch(
    val batchId: String,
    val inputFileId: String,
    val model: String,
    var status: String = "validating",
    var outputFileId: String? = null,
    var errorFileId: String? = null,
    val createdAt: Long,
    var inProgressAt: Long? = null,
    var finalizingAt: Long? = null,
    var completedAt: Long? = null,
    var failedAt: Long? = null,
    var expiredAt: Long? = null,
    val inputContent: ByteArray,
    var requestCounts: RequestCounts? = null,
    var error: BatchApiError? = null,
  )

  private val fileStore = ConcurrentHashMap<String, ByteArray>()
  private val batchStore = ConcurrentHashMap<String, FakeBatch>()
  private val idCounter = AtomicInteger(0)

  // -- Reset ----------------------------------------------------------------

  /** Call in @BeforeEach to restore clean state. */
  fun reset() {
    instantCompletion = false
    completionDelayMs = 0
    nextCreateError = null
    nextPollError = null
    failedItemCount = 0
    stuckInValidating = false
    submissions.clear()
    pollCalls.clear()
    cancelCalls.clear()
    deletedFiles.clear()
    fileStore.clear()
    batchStore.clear()
    idCounter.set(0)
  }

  // -- Manual helpers -------------------------------------------------------

  /** Force-complete a batch. */
  fun completeBatch(batchId: String) {
    val batch = batchStore[batchId] ?: error("No batch found: $batchId")
    finishBatch(batch, "completed")
  }

  /** Force-fail a batch. */
  fun failBatch(
    batchId: String,
    errorMessage: String = "Simulated failure",
  ) {
    val batch = batchStore[batchId] ?: error("No batch found: $batchId")
    batch.status = "failed"
    batch.failedAt = nowEpoch()
    batch.error = BatchApiError(code = "server_error", message = errorMessage)
  }

  /** Force-expire a batch. */
  fun expireBatch(batchId: String) {
    val batch = batchStore[batchId] ?: error("No batch found: $batchId")
    batch.status = "expired"
    batch.expiredAt = nowEpoch()
  }

  // -- OpenAiBatchApiService implementation ---------------------------------

  override fun submitBatch(
    apiKey: String,
    apiUrl: String,
    jsonlContent: ByteArray,
    endpoint: String,
    model: String,
    metadata: Map<String, String>?,
  ): BatchSubmissionResult {
    nextCreateError?.let {
      nextCreateError = null
      throw it
    }

    val fileId = "file-fake-${idCounter.incrementAndGet()}"
    val batchId = "batch_fake-${idCounter.incrementAndGet()}"
    val now = nowEpoch()

    fileStore[fileId] = jsonlContent

    val batch =
      FakeBatch(
        batchId = batchId,
        inputFileId = fileId,
        model = model,
        createdAt = now,
        inputContent = jsonlContent,
      )

    if (instantCompletion) {
      finishBatch(batch, "completed")
    }

    batchStore[batchId] = batch

    submissions.add(
      Submission(
        apiKey = apiKey,
        apiUrl = apiUrl,
        jsonlContent = jsonlContent,
        endpoint = endpoint,
        model = model,
        metadata = metadata,
        batchId = batchId,
      ),
    )

    return BatchSubmissionResult(
      batchId = batchId,
      inputFileId = fileId,
    )
  }

  override fun pollBatchStatus(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult {
    nextPollError?.let {
      nextPollError = null
      throw it
    }

    pollCalls.add(PollCall(apiKey = apiKey, apiUrl = apiUrl, batchId = batchId))

    val batch =
      batchStore[batchId]
        ?: return BatchStatusResult(
          batchId = batchId,
          status = "failed",
          error = BatchApiError(code = "not_found", message = "Batch not found: $batchId"),
        )

    advanceState(batch)

    return toBatchStatusResult(batch)
  }

  override fun downloadResults(
    apiKey: String,
    apiUrl: String,
    outputFileId: String,
  ): ByteArray {
    return fileStore[outputFileId]
      ?: throw IllegalStateException("Output file not found: $outputFileId")
  }

  override fun downloadErrors(
    apiKey: String,
    apiUrl: String,
    errorFileId: String,
  ): ByteArray? {
    return fileStore[errorFileId]
  }

  override fun cancelBatch(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult {
    cancelCalls.add(batchId)

    val batch =
      batchStore[batchId]
        ?: return BatchStatusResult(
          batchId = batchId,
          status = "failed",
          error = BatchApiError(code = "not_found", message = "Batch not found: $batchId"),
        )

    batch.status = "cancelling"
    // In the real API, cancelling transitions to cancelled asynchronously.
    // For tests, we immediately set it to cancelled.
    batch.status = "cancelled"

    return toBatchStatusResult(batch)
  }

  override fun deleteFile(
    apiKey: String,
    apiUrl: String,
    fileId: String,
  ) {
    deletedFiles.add(fileId)
    fileStore.remove(fileId)
  }

  // -- Internal helpers -----------------------------------------------------

  private fun advanceState(batch: FakeBatch) {
    if (stuckInValidating && batch.status == "validating") return

    val now = nowEpoch()

    when (batch.status) {
      "validating" -> {
        batch.status = "in_progress"
        batch.inProgressAt = now
      }
      "in_progress" -> {
        if (completionDelayMs > 0) {
          val elapsedMs = (now - (batch.inProgressAt ?: now)) * 1000
          if (elapsedMs >= completionDelayMs) {
            batch.status = "finalizing"
            batch.finalizingAt = now
          }
        } else {
          batch.status = "finalizing"
          batch.finalizingAt = now
        }
      }
      "finalizing" -> {
        finishBatch(batch, "completed")
      }
    }
  }

  private fun finishBatch(
    batch: FakeBatch,
    terminalStatus: String,
  ) {
    val now = nowEpoch()
    val outputFileId = "file-fake-${idCounter.incrementAndGet()}"
    val outputContent = generateOutput(batch.inputContent, batch.model, now)
    fileStore[outputFileId] = outputContent

    val lineCount =
      batch.inputContent
        .toString(Charsets.UTF_8)
        .lines()
        .count { it.isNotBlank() }

    batch.status = terminalStatus
    batch.completedAt = now
    batch.outputFileId = outputFileId
    batch.requestCounts =
      RequestCounts(
        total = lineCount,
        completed = lineCount - failedItemCount,
        failed = failedItemCount,
      )

    if (batch.inProgressAt == null) {
      batch.inProgressAt = now
    }
  }

  private fun toBatchStatusResult(batch: FakeBatch): BatchStatusResult =
    BatchStatusResult(
      batchId = batch.batchId,
      status = batch.status,
      outputFileId = batch.outputFileId,
      errorFileId = batch.errorFileId,
      error = batch.error,
      requestCounts = batch.requestCounts,
      createdAt = batch.createdAt,
      inProgressAt = batch.inProgressAt,
      completedAt = batch.completedAt,
      failedAt = batch.failedAt,
      expiredAt = batch.expiredAt,
    )

  private fun nowEpoch(): Long = currentDateProvider.date.time / 1000

  private fun generateOutput(
    inputContent: ByteArray,
    model: String,
    createdAt: Long,
  ): ByteArray {
    val lines = inputContent.toString(Charsets.UTF_8).lines().filter { it.isNotBlank() }
    val outputLines =
      lines.mapIndexed { index, line ->
        val customId = extractCustomId(line)
        buildOutputLine(index + 1, customId, model, createdAt)
      }
    return outputLines.joinToString("\n").toByteArray(Charsets.UTF_8)
  }

  private fun extractCustomId(jsonLine: String): String {
    val regex = """"custom_id"\s*:\s*"([^"]+)"""".toRegex()
    return regex.find(jsonLine)?.groupValues?.get(1) ?: "unknown"
  }

  private fun buildOutputLine(
    index: Int,
    customId: String,
    model: String,
    createdAt: Long,
  ): String {
    val responseBody =
      """{"id":"chatcmpl-fake-$index","object":"chat.completion","created":$createdAt,""" +
        """"model":"$model","choices":[{"index":0,"message":{"role":"assistant",""" +
        """"content":"{\"output\":\"translated: $customId\",""" +
        """"contextDescription\":\"batch translation\"}"},""" +
        """"finish_reason":"stop"}],"usage":{"prompt_tokens":50,""" +
        """"completion_tokens":15,"total_tokens":65},"system_fingerprint":"fp_fake_mock"}"""

    return """{"id":"batch_req_$index","custom_id":"$customId",""" +
      """"response":{"status_code":200,"request_id":"req_fake_$index",""" +
      """"body":$responseBody},"error":null}"""
  }
}
