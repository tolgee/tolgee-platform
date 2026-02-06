package io.tolgee.ee.unit.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.ee.service.batch.OpenAiBatchPoller
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.project.ProjectService
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.transaction.PlatformTransactionManager

/**
 * Additional parseResults tests beyond those in OpenAiBatchPollerTest.
 * Covers edge cases around response structure variations.
 */
class OpenAiBatchPollerParseResultsTest {
  private lateinit var poller: OpenAiBatchPoller
  private val objectMapper = jacksonObjectMapper()

  @BeforeEach
  fun setUp() {
    poller =
      OpenAiBatchPoller(
        schedulingManager = mock(SchedulingManager::class.java),
        lockingProvider = mock(LockingProvider::class.java),
        openAiBatchApiService = mock(OpenAiBatchApiService::class.java),
        openAiBatchJobTrackerRepository = mock(OpenAiBatchJobTrackerRepository::class.java),
        entityManager = mock(EntityManager::class.java),
        transactionManager = mock(PlatformTransactionManager::class.java),
        batchProperties = BatchProperties(),
        objectMapper = objectMapper,
        batchJobChunkExecutionQueue = mock(BatchJobChunkExecutionQueue::class.java),
        projectService = mock(ProjectService::class.java),
        llmProviderService = mock(LlmProviderService::class.java),
        metrics = mock(io.tolgee.Metrics::class.java),
        progressManager = mock(io.tolgee.batch.ProgressManager::class.java),
      )
  }

  @Test
  fun `handles HTTP error status code in response`() {
    // OpenAI returns a 400 in the response object (not the batch-level error field)
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":400,"request_id":"req_1","body":{"error":{"message":"Invalid request","type":"invalid_request_error","code":"invalid_model"}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    // Should still parse (choices is absent, so no translation), but line parsed without crash
    results.assert.hasSize(0)
  }

  @Test
  fun `handles response with empty choices array`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[],"usage":{"prompt_tokens":10,"completion_tokens":0,"total_tokens":10}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    // Empty choices -> get(0) returns null -> result skipped
    results.assert.hasSize(0)
  }

  @Test
  fun `handles response with null content in message`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":null},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":0,"total_tokens":10}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].translatedText.assert.isNull()
  }

  @Test
  fun `handles response with missing message field`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":0,"total_tokens":10}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    // Missing message -> returns null -> skipped
    results.assert.hasSize(0)
  }

  @Test
  fun `handles response with missing body field`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1"},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(0)
  }

  @Test
  fun `handles response with missing response field entirely`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(0)
  }

  @Test
  fun `parses custom_id with extra colon-separated fields`() {
    // custom_id has more than 3 parts; should still work (parts[1] and parts[2])
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5:extra:data","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Result"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].keyId.assert.isEqualTo(100L)
    results[0].languageId.assert.isEqualTo(5L)
  }

  @Test
  fun `handles mixed valid and invalid lines`() {
    val jsonl =
      listOf(
        """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Good"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}""",
        "not json at all",
        """{"id":"batch_req_2","custom_id":"bad-id","response":{"status_code":200,"request_id":"req_2","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Ignored"},"finish_reason":"stop"}]}},"error":null}""",
        """{"id":"batch_req_3","custom_id":"42:101:6","response":{"status_code":200,"request_id":"req_3","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Also good"},"finish_reason":"stop"}],"usage":{"prompt_tokens":20,"completion_tokens":8,"total_tokens":28}}},"error":null}""",
      ).joinToString("\n")

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(2)
    results[0].translatedText.assert.isEqualTo("Good")
    results[1].translatedText.assert.isEqualTo("Also good")
  }

  @Test
  fun `handles error with missing message field`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":null,"error":{"code":"server_error"}}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].error.assert.isEqualTo("Unknown error")
    results[0].translatedText.assert.isNull()
  }

  @Test
  fun `handles custom_id with only two parts`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Result"},"finish_reason":"stop"}]}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    // Less than 3 parts -> skipped
    results.assert.hasSize(0)
  }

  @Test
  fun `handles response with multiline translated text`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Line 1\nLine 2\nLine 3"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":15,"total_tokens":25}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    // Jackson parses the JSON escape \n into real newlines
    results[0].translatedText.assert.isEqualTo("Line 1\nLine 2\nLine 3")
  }

  @Test
  fun `handles response with unicode content`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"\u4f60\u597d\u4e16\u754c"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    // Unicode for Chinese characters
    results[0].translatedText.assert.isNotNull()
  }

  @Test
  fun `handles large token counts`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Big result"},"finish_reason":"stop"}],"usage":{"prompt_tokens":999999999,"completion_tokens":888888888,"total_tokens":1888888887}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].promptTokens.assert.isEqualTo(999999999L)
    results[0].completionTokens.assert.isEqualTo(888888888L)
  }
}
