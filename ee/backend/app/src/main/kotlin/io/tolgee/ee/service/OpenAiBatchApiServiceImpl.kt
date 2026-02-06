package io.tolgee.ee.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.ee.component.FakeBatchApiDelegate
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

/**
 * Production implementation of [OpenAiBatchApiService] using [RestTemplate].
 *
 * Calls the OpenAI Files API (`/v1/files`) and Batches API (`/v1/batches`).
 * When [InternalProperties.fakeBatchApi] is `true`, all calls are delegated
 * to [FakeBatchApiDelegate] for local development and E2E testing.
 */
@Service
class OpenAiBatchApiServiceImpl(
  private val restTemplate: RestTemplate,
  private val objectMapper: ObjectMapper,
  private val internalProperties: InternalProperties,
  @Lazy
  private val fakeBatchApiDelegate: FakeBatchApiDelegate?,
) : OpenAiBatchApiService,
  Logging {
  override fun submitBatch(
    apiKey: String,
    apiUrl: String,
    jsonlContent: ByteArray,
    endpoint: String,
    model: String,
    metadata: Map<String, String>?,
  ): BatchSubmissionResult {
    if (internalProperties.fakeBatchApi) {
      logger.debug("Fake batch API mode: submitting batch")
      return requireFakeDelegate().submitBatch(apiKey, apiUrl, jsonlContent, endpoint, model, metadata)
    }

    val inputFileId = uploadFile(apiKey, apiUrl, jsonlContent)
    val batchId = createBatch(apiKey, apiUrl, inputFileId, endpoint, metadata)

    return BatchSubmissionResult(
      batchId = batchId,
      inputFileId = inputFileId,
    )
  }

  override fun pollBatchStatus(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult {
    if (internalProperties.fakeBatchApi) {
      logger.debug("Fake batch API mode: polling batch status for {}", batchId)
      return requireFakeDelegate().pollBatchStatus(apiKey, apiUrl, batchId)
    }

    val url = "$apiUrl/v1/batches/$batchId"
    val response = executeGet(apiKey, url)
    return parseBatchStatusResponse(response)
  }

  override fun downloadResults(
    apiKey: String,
    apiUrl: String,
    outputFileId: String,
  ): ByteArray {
    if (internalProperties.fakeBatchApi) {
      logger.debug("Fake batch API mode: downloading results for file {}", outputFileId)
      return requireFakeDelegate().downloadResults(apiKey, apiUrl, outputFileId)
    }

    return downloadFileContent(apiKey, apiUrl, outputFileId)
  }

  override fun downloadErrors(
    apiKey: String,
    apiUrl: String,
    errorFileId: String,
  ): ByteArray? {
    if (internalProperties.fakeBatchApi) {
      logger.debug("Fake batch API mode: downloading errors for file {}", errorFileId)
      return requireFakeDelegate().downloadErrors(apiKey, apiUrl, errorFileId)
    }

    return try {
      downloadFileContent(apiKey, apiUrl, errorFileId)
    } catch (e: HttpClientErrorException.NotFound) {
      logger.debug("No error file found for file ID {}", errorFileId)
      null
    }
  }

  override fun cancelBatch(
    apiKey: String,
    apiUrl: String,
    batchId: String,
  ): BatchStatusResult {
    if (internalProperties.fakeBatchApi) {
      logger.debug("Fake batch API mode: cancelling batch {}", batchId)
      return requireFakeDelegate().cancelBatch(apiKey, apiUrl, batchId)
    }

    val url = "$apiUrl/v1/batches/$batchId/cancel"
    val headers = buildAuthHeaders(apiKey)
    headers.contentType = MediaType.APPLICATION_JSON

    val request = HttpEntity<Any>(null, headers)
    val response =
      restTemplate.exchange(
        url,
        HttpMethod.POST,
        request,
        String::class.java,
      )

    return parseBatchStatusResponse(response.body ?: throw IllegalStateException("Empty response from cancel batch"))
  }

  override fun deleteFile(
    apiKey: String,
    apiUrl: String,
    fileId: String,
  ) {
    if (internalProperties.fakeBatchApi) {
      logger.debug("Fake batch API mode: deleting file {}", fileId)
      requireFakeDelegate().deleteFile(apiKey, apiUrl, fileId)
      return
    }

    val url = "$apiUrl/v1/files/$fileId"
    val headers = buildAuthHeaders(apiKey)
    val request = HttpEntity<Any>(null, headers)

    restTemplate.exchange(
      url,
      HttpMethod.DELETE,
      request,
      String::class.java,
    )
  }

  private fun uploadFile(
    apiKey: String,
    apiUrl: String,
    jsonlContent: ByteArray,
  ): String {
    val url = "$apiUrl/v1/files"

    val headers = buildAuthHeaders(apiKey)
    headers.contentType = MediaType.MULTIPART_FORM_DATA

    val fileResource =
      object : ByteArrayResource(jsonlContent) {
        override fun getFilename(): String = "batch_input.jsonl"
      }

    val body =
      LinkedMultiValueMap<String, Any>().apply {
        add("purpose", "batch")
        add("file", fileResource)
      }

    val request = HttpEntity(body, headers)

    val response =
      restTemplate.exchange(
        url,
        HttpMethod.POST,
        request,
        String::class.java,
      )

    val responseBody =
      response.body ?: throw IllegalStateException("Empty response from file upload")
    val responseMap = objectMapper.readValue<Map<String, Any>>(responseBody)
    return responseMap["id"] as? String
      ?: throw IllegalStateException("No file ID in upload response")
  }

  private fun createBatch(
    apiKey: String,
    apiUrl: String,
    inputFileId: String,
    endpoint: String,
    metadata: Map<String, String>?,
  ): String {
    val url = "$apiUrl/v1/batches"

    val headers = buildAuthHeaders(apiKey)
    headers.contentType = MediaType.APPLICATION_JSON

    val requestBody =
      mutableMapOf<String, Any>(
        "input_file_id" to inputFileId,
        "endpoint" to endpoint,
        "completion_window" to "24h",
      )
    if (metadata != null) {
      requestBody["metadata"] = metadata
    }

    val request = HttpEntity(objectMapper.writeValueAsString(requestBody), headers)

    val response =
      restTemplate.exchange(
        url,
        HttpMethod.POST,
        request,
        String::class.java,
      )

    val responseBody =
      response.body ?: throw IllegalStateException("Empty response from batch creation")
    val responseMap = objectMapper.readValue<Map<String, Any>>(responseBody)
    return responseMap["id"] as? String
      ?: throw IllegalStateException("No batch ID in creation response")
  }

  private fun downloadFileContent(
    apiKey: String,
    apiUrl: String,
    fileId: String,
  ): ByteArray {
    val url = "$apiUrl/v1/files/$fileId/content"
    val headers = buildAuthHeaders(apiKey)
    val request = HttpEntity<Any>(null, headers)

    val response =
      restTemplate.exchange(
        url,
        HttpMethod.GET,
        request,
        ByteArray::class.java,
      )

    return response.body ?: throw IllegalStateException("Empty response when downloading file $fileId")
  }

  private fun executeGet(
    apiKey: String,
    url: String,
  ): String {
    val headers = buildAuthHeaders(apiKey)
    val request = HttpEntity<Any>(null, headers)

    val response: ResponseEntity<String> =
      restTemplate.exchange(
        url,
        HttpMethod.GET,
        request,
        String::class.java,
      )

    return response.body ?: throw IllegalStateException("Empty response from GET $url")
  }

  private fun buildAuthHeaders(apiKey: String): HttpHeaders {
    return HttpHeaders().apply {
      set("Authorization", "Bearer $apiKey")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseBatchStatusResponse(responseBody: String): BatchStatusResult {
    val map = objectMapper.readValue<Map<String, Any?>>(responseBody)

    val errorMap = map["errors"] as? Map<String, Any?>
    val errorData =
      (errorMap?.get("data") as? List<Map<String, Any?>>)?.firstOrNull()
    val error =
      errorData?.let {
        BatchApiError(
          code = it["code"] as? String ?: "",
          message = it["message"] as? String ?: "",
          param = it["param"] as? String,
          line = (it["line"] as? Number)?.toInt(),
        )
      }

    val requestCountsMap = map["request_counts"] as? Map<String, Any?>
    val requestCounts =
      requestCountsMap?.let {
        RequestCounts(
          total = (it["total"] as? Number)?.toInt() ?: 0,
          completed = (it["completed"] as? Number)?.toInt() ?: 0,
          failed = (it["failed"] as? Number)?.toInt() ?: 0,
        )
      }

    return BatchStatusResult(
      batchId = map["id"] as? String ?: "",
      status = map["status"] as? String ?: "",
      outputFileId = map["output_file_id"] as? String,
      errorFileId = map["error_file_id"] as? String,
      error = error,
      requestCounts = requestCounts,
      createdAt = (map["created_at"] as? Number)?.toLong(),
      inProgressAt = (map["in_progress_at"] as? Number)?.toLong(),
      completedAt = (map["completed_at"] as? Number)?.toLong(),
      failedAt = (map["failed_at"] as? Number)?.toLong(),
      expiredAt = (map["expired_at"] as? Number)?.toLong(),
    )
  }

  private fun requireFakeDelegate(): FakeBatchApiDelegate {
    return fakeBatchApiDelegate
      ?: throw IllegalStateException("FakeBatchApiDelegate not available")
  }
}
