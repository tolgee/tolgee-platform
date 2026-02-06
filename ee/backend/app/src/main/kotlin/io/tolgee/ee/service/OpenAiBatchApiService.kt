package io.tolgee.ee.service

/**
 * Abstraction over the OpenAI Batch API lifecycle.
 * Production: makes real HTTP calls.
 * Test: replaced by FakeOpenAiBatchApiService.
 * E2E/Dev: delegates to in-process fake when fakeBatchApi=true.
 */
interface OpenAiBatchApiService {
  /**
   * Upload a JSONL file and create a batch job.
   * Combines Files API upload + Batches API create into one call,
   * since callers never need the file ID independently.
   */
  fun submitBatch(
    apiKey: String,
    apiUrl: String,
    jsonlContent: ByteArray,
    endpoint: String = "/v1/chat/completions",
    model: String,
    metadata: Map<String, String>? = null,
  ): BatchSubmissionResult

  /**
   * Poll the status of an existing batch.
   */
  fun pollBatchStatus(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult

  /**
   * Download the output JSONL file for a completed batch.
   */
  fun downloadResults(
    apiKey: String,
    apiUrl: String,
    outputFileId: String,
  ): ByteArray

  /**
   * Download the error JSONL file for a completed/expired batch.
   * Returns null if no error file exists.
   */
  fun downloadErrors(
    apiKey: String,
    apiUrl: String,
    errorFileId: String,
  ): ByteArray?

  /**
   * Cancel a running batch.
   */
  fun cancelBatch(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult

  /**
   * Delete uploaded files (input, output, error) for cleanup.
   */
  fun deleteFile(
    apiKey: String,
    apiUrl: String,
    fileId: String,
  )
}

data class BatchSubmissionResult(
  val batchId: String,
  val inputFileId: String,
)

data class BatchStatusResult(
  val batchId: String,
  val status: String,
  val outputFileId: String? = null,
  val errorFileId: String? = null,
  val error: BatchApiError? = null,
  val requestCounts: RequestCounts? = null,
  val createdAt: Long? = null,
  val inProgressAt: Long? = null,
  val completedAt: Long? = null,
  val failedAt: Long? = null,
  val expiredAt: Long? = null,
)

data class BatchApiError(
  val code: String,
  val message: String,
  val param: String? = null,
  val line: Int? = null,
)

data class RequestCounts(
  val total: Int,
  val completed: Int,
  val failed: Int,
)
