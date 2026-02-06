package io.tolgee.ee.component

import io.tolgee.ee.service.BatchStatusResult
import io.tolgee.ee.service.BatchSubmissionResult

/**
 * In-process fake implementation of the OpenAI Batch API for E2E/dev usage.
 * When [io.tolgee.configuration.tolgee.InternalProperties.fakeBatchApi] is true,
 * [io.tolgee.ee.service.OpenAiBatchApiServiceImpl] delegates all calls to this component.
 */
interface FakeBatchApiDelegate {
  fun submitBatch(
    apiKey: String,
    apiUrl: String,
    jsonlContent: ByteArray,
    endpoint: String,
    model: String,
    metadata: Map<String, String>?,
  ): BatchSubmissionResult

  fun pollBatchStatus(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult

  fun downloadResults(
    apiKey: String,
    apiUrl: String,
    outputFileId: String,
  ): ByteArray

  fun downloadErrors(
    apiKey: String,
    apiUrl: String,
    errorFileId: String,
  ): ByteArray?

  fun cancelBatch(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult

  fun deleteFile(
    apiKey: String,
    apiUrl: String,
    fileId: String,
  )
}
