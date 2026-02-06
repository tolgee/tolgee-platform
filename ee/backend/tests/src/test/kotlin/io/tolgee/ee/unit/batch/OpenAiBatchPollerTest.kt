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

class OpenAiBatchPollerTest {
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
        metrics = mock(io.tolgee.Metrics::class.java, org.mockito.Mockito.RETURNS_DEEP_STUBS),
        progressManager = mock(io.tolgee.batch.ProgressManager::class.java),
      )
  }

  @Test
  fun `parses valid JSONL result lines`() {
    val jsonl =
      listOf(
        """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"id":"chatcmpl-1","object":"chat.completion","created":1700000000,"model":"gpt-4o-mini","choices":[{"index":0,"message":{"role":"assistant","content":"Hello world"},"finish_reason":"stop"}],"usage":{"prompt_tokens":50,"completion_tokens":15,"total_tokens":65}}},"error":null}""",
        """{"id":"batch_req_2","custom_id":"42:101:6","response":{"status_code":200,"request_id":"req_2","body":{"id":"chatcmpl-2","object":"chat.completion","created":1700000000,"model":"gpt-4o-mini","choices":[{"index":0,"message":{"role":"assistant","content":"Bonjour monde"},"finish_reason":"stop"}],"usage":{"prompt_tokens":55,"completion_tokens":18,"total_tokens":73}}},"error":null}""",
      ).joinToString("\n")

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(2)

    results[0].let {
      it.customId.assert.isEqualTo("42:100:5")
      it.keyId.assert.isEqualTo(100L)
      it.languageId.assert.isEqualTo(5L)
      it.translatedText.assert.isEqualTo("Hello world")
      it.promptTokens.assert.isEqualTo(50L)
      it.completionTokens.assert.isEqualTo(15L)
      it.error.assert.isNull()
    }

    results[1].let {
      it.customId.assert.isEqualTo("42:101:6")
      it.keyId.assert.isEqualTo(101L)
      it.languageId.assert.isEqualTo(6L)
      it.translatedText.assert.isEqualTo("Bonjour monde")
      it.promptTokens.assert.isEqualTo(55L)
      it.completionTokens.assert.isEqualTo(18L)
    }
  }

  @Test
  fun `handles error results from OpenAI`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":null,"error":{"code":"server_error","message":"Internal error processing request"}}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].let {
      it.customId.assert.isEqualTo("42:100:5")
      it.keyId.assert.isEqualTo(100L)
      it.languageId.assert.isEqualTo(5L)
      it.translatedText.assert.isNull()
      it.error.assert.isEqualTo("Internal error processing request")
    }
  }

  @Test
  fun `skips malformed lines gracefully`() {
    val jsonl =
      listOf(
        "this is not valid json",
        """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Valid"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}""",
      ).joinToString("\n")

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].translatedText.assert.isEqualTo("Valid")
  }

  @Test
  fun `skips lines with invalid custom_id format`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"invalid-format","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Result"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(0)
  }

  @Test
  fun `skips lines with non-numeric keyId or languageId`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:abc:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Result"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(0)
  }

  @Test
  fun `handles empty input`() {
    val results = poller.parseResults(ByteArray(0))
    results.assert.hasSize(0)
  }

  @Test
  fun `handles blank lines in input`() {
    val jsonl = "\n\n\n"
    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))
    results.assert.hasSize(0)
  }

  @Test
  fun `defaults token counts to zero when usage is missing`() {
    val jsonl =
      """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Translated"},"finish_reason":"stop"}]}},"error":null}"""

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].let {
      it.translatedText.assert.isEqualTo("Translated")
      it.promptTokens.assert.isEqualTo(0L)
      it.completionTokens.assert.isEqualTo(0L)
    }
  }
}
