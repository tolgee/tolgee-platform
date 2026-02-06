package io.tolgee.ee.component

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.ee.service.BatchApiError
import io.tolgee.ee.service.BatchStatusResult
import io.tolgee.ee.service.BatchSubmissionResult
import io.tolgee.ee.service.RequestCounts
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-process batch simulation for E2E tests and local development.
 * Used by [io.tolgee.ee.service.OpenAiBatchApiServiceImpl] when
 * [InternalProperties.fakeBatchApi] is true.
 *
 * Provides a simple state machine: validating -> in_progress -> completed,
 * with a configurable completion delay from [InternalProperties.fakeBatchApiCompletionDelayMs].
 */
@Component
class FakeBatchApiDelegateImpl(
  private val currentDateProvider: CurrentDateProvider,
  private val internalProperties: InternalProperties,
) : FakeBatchApiDelegate {
  private val fileStore = ConcurrentHashMap<String, ByteArray>()
  private val batchStore = ConcurrentHashMap<String, DelegateBatch>()
  private val idCounter = AtomicInteger(0)

  data class DelegateBatch(
    val batchId: String,
    val inputFileId: String,
    val endpoint: String,
    val model: String,
    val metadata: Map<String, String>?,
    var status: String = "validating",
    var outputFileId: String? = null,
    var errorFileId: String? = null,
    val createdAt: Long,
    var inProgressAt: Long? = null,
    var completedAt: Long? = null,
    val inputContent: ByteArray,
    var requestCounts: RequestCounts? = null,
  )

  override fun submitBatch(
    apiKey: String,
    apiUrl: String,
    jsonlContent: ByteArray,
    endpoint: String,
    model: String,
    metadata: Map<String, String>?,
  ): BatchSubmissionResult {
    val fileId = "file-fake-${idCounter.incrementAndGet()}"
    val batchId = "batch_fake-${idCounter.incrementAndGet()}"

    fileStore[fileId] = jsonlContent

    val now = currentDateProvider.date.time / 1000

    val batch =
      DelegateBatch(
        batchId = batchId,
        inputFileId = fileId,
        endpoint = endpoint,
        model = model,
        metadata = metadata,
        createdAt = now,
        inputContent = jsonlContent,
      )
    batchStore[batchId] = batch

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
    val batch =
      batchStore[batchId]
        ?: return BatchStatusResult(
          batchId = batchId,
          status = "failed",
          error = BatchApiError(code = "not_found", message = "Batch not found: $batchId"),
        )

    advanceState(batch)

    return BatchStatusResult(
      batchId = batch.batchId,
      status = batch.status,
      outputFileId = batch.outputFileId,
      errorFileId = batch.errorFileId,
      requestCounts = batch.requestCounts,
      createdAt = batch.createdAt,
      inProgressAt = batch.inProgressAt,
      completedAt = batch.completedAt,
    )
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
    val batch =
      batchStore[batchId]
        ?: return BatchStatusResult(
          batchId = batchId,
          status = "failed",
          error = BatchApiError(code = "not_found", message = "Batch not found: $batchId"),
        )

    batch.status = "cancelled"
    return BatchStatusResult(
      batchId = batch.batchId,
      status = batch.status,
      requestCounts = batch.requestCounts,
      createdAt = batch.createdAt,
      inProgressAt = batch.inProgressAt,
    )
  }

  override fun deleteFile(
    apiKey: String,
    apiUrl: String,
    fileId: String,
  ) {
    fileStore.remove(fileId)
  }

  private fun advanceState(batch: DelegateBatch) {
    val now = currentDateProvider.date.time / 1000
    val delaySeconds = internalProperties.fakeBatchApiCompletionDelayMs / 1000

    when (batch.status) {
      "validating" -> {
        batch.status = "in_progress"
        batch.inProgressAt = now
      }
      "in_progress" -> {
        val elapsed = now - (batch.inProgressAt ?: now)
        if (elapsed >= delaySeconds) {
          completeBatch(batch, now)
        }
      }
    }
  }

  private fun completeBatch(
    batch: DelegateBatch,
    now: Long,
  ) {
    val outputFileId = "file-fake-${idCounter.incrementAndGet()}"
    val outputContent = generateOutput(batch.inputContent, batch.model, now)
    fileStore[outputFileId] = outputContent

    val lineCount =
      batch.inputContent
        .toString(Charsets.UTF_8)
        .lines()
        .count { it.isNotBlank() }

    batch.status = "completed"
    batch.completedAt = now
    batch.outputFileId = outputFileId
    batch.requestCounts = RequestCounts(total = lineCount, completed = lineCount, failed = 0)
  }

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
