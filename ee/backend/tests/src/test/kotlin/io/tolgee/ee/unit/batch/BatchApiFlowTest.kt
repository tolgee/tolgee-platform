package io.tolgee.ee.unit.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.WaitingForExternalException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.LockingProvider
import io.tolgee.component.SchedulingManager
import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.ee.service.BatchSubmissionResult
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.ee.service.batch.BatchApiResultApplier
import io.tolgee.ee.service.batch.BatchApiSubmissionService
import io.tolgee.ee.service.batch.OpenAiBatchPoller
import io.tolgee.model.Language
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchResult
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.key.Key
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.request.prompt.PromptDto
import io.tolgee.ee.service.prompt.PromptServiceEeImpl
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.PlatformTransactionManager

/**
 * Tests the two-phase batch API flow:
 *  Phase 1 (submission): BatchApiSubmissionService builds JSONL, submits to OpenAI, creates tracker
 *  Phase 2 (result application): BatchApiResultApplier reads tracker results and applies translations
 *
 * Also tests the poller's result parsing and the chunk processor's routing logic.
 */
class BatchApiFlowTest {
  private val objectMapper = jacksonObjectMapper()

  // Mocked dependencies
  private lateinit var openAiBatchApiService: OpenAiBatchApiService
  private lateinit var trackerRepository: OpenAiBatchJobTrackerRepository
  private lateinit var entityManager: EntityManager
  private lateinit var llmProviderService: LlmProviderService
  private lateinit var projectService: ProjectService
  private lateinit var keyService: KeyService
  private lateinit var languageService: LanguageService
  private lateinit var translationService: TranslationService
  private lateinit var progressManager: ProgressManager
  private lateinit var transactionManager: PlatformTransactionManager

  private lateinit var promptService: PromptServiceEeImpl

  // Services under test
  private lateinit var submissionService: BatchApiSubmissionService
  private lateinit var resultApplier: BatchApiResultApplier
  private lateinit var poller: OpenAiBatchPoller

  @BeforeEach
  fun setUp() {
    openAiBatchApiService = mock(OpenAiBatchApiService::class.java)
    trackerRepository = mock(OpenAiBatchJobTrackerRepository::class.java)
    entityManager = mock(EntityManager::class.java)
    llmProviderService = mock(LlmProviderService::class.java)
    projectService = mock(ProjectService::class.java)
    keyService = mock(KeyService::class.java)
    languageService = mock(LanguageService::class.java)
    translationService = mock(TranslationService::class.java)
    progressManager = mock(ProgressManager::class.java)
    transactionManager = mock(PlatformTransactionManager::class.java)
    promptService = mock(PromptServiceEeImpl::class.java)

    val metrics = mock(io.tolgee.Metrics::class.java, org.mockito.Mockito.RETURNS_DEEP_STUBS)
    val mtCreditsService = mock(io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService::class.java)
    val batchApiValidationService = mock(io.tolgee.ee.service.batch.BatchApiValidationService::class.java)

    setupPromptServiceDefaults()

    submissionService =
      BatchApiSubmissionService(
        openAiBatchApiService = openAiBatchApiService,
        openAiBatchJobTrackerRepository = trackerRepository,
        entityManager = entityManager,
        objectMapper = objectMapper,
        llmProviderService = llmProviderService,
        projectService = projectService,
        keyService = keyService,
        languageService = languageService,
        mtCreditsService = mtCreditsService,
        batchApiValidationService = batchApiValidationService,
        metrics = metrics,
        promptService = promptService,
      )

    resultApplier =
      BatchApiResultApplier(
        openAiBatchJobTrackerRepository = trackerRepository,
        translationService = translationService,
        progressManager = progressManager,
        languageService = languageService,
        keyService = keyService,
        mtCreditsService = mtCreditsService,
        llmProviderService = llmProviderService,
        projectService = projectService,
        metrics = metrics,
      )

    poller =
      OpenAiBatchPoller(
        schedulingManager = mock(SchedulingManager::class.java),
        lockingProvider = mock(LockingProvider::class.java),
        openAiBatchApiService = openAiBatchApiService,
        openAiBatchJobTrackerRepository = trackerRepository,
        entityManager = entityManager,
        transactionManager = transactionManager,
        batchProperties = BatchProperties(),
        objectMapper = objectMapper,
        batchJobChunkExecutionQueue = mock(BatchJobChunkExecutionQueue::class.java),
        projectService = projectService,
        llmProviderService = llmProviderService,
        metrics = metrics,
        progressManager = mock(io.tolgee.batch.ProgressManager::class.java),
      )
  }

  @Test
  fun `submission service builds JSONL and creates tracker`() {
    val projectId = 1L
    val batchId = "batch_abc123"
    val fileId = "file_xyz789"

    val job = createJobDto(projectId = projectId, jobId = 42L)
    val chunk = listOf(BatchTranslationTargetItem(100L, 5L))
    val chunkExecution = createChunkExecution(jobId = 42L)

    setupProviderConfig(projectId)
    setupKeysAndLanguages(
      keys = mapOf(100L to createKey(100L, "greeting")),
      languages = mapOf(5L to createLanguage(5L, "cs")),
    )

    whenever(
      openAiBatchApiService.submitBatch(
        apiKey = any(),
        apiUrl = any(),
        jsonlContent = any(),
        endpoint = any(),
        model = any(),
        metadata = anyOrNull(),
      ),
    ).thenReturn(BatchSubmissionResult(batchId = batchId, inputFileId = fileId))

    // Submission should throw WaitingForExternalException
    val exception =
      assertThrows<WaitingForExternalException> {
        submissionService.submitBatch(job, chunk, chunkExecution)
      }
    exception.message.assert.contains(batchId)

    // Verify tracker was saved
    verify(trackerRepository).save(
      argThat<OpenAiBatchJobTracker> {
        openAiBatchId == batchId &&
          openAiInputFileId == fileId &&
          status == OpenAiBatchTrackerStatus.SUBMITTED &&
          totalRequests == 1
      },
    )
  }

  @Test
  fun `submission JSONL content has correct format`() {
    val projectId = 1L
    val job = createJobDto(projectId = projectId, jobId = 42L)
    val chunk =
      listOf(
        BatchTranslationTargetItem(100L, 5L),
        BatchTranslationTargetItem(101L, 6L),
      )
    val chunkExecution = createChunkExecution(jobId = 42L)

    setupProviderConfig(projectId)
    setupKeysAndLanguages(
      keys =
        mapOf(
          100L to createKey(100L, "hello"),
          101L to createKey(101L, "goodbye"),
        ),
      languages =
        mapOf(
          5L to createLanguage(5L, "cs"),
          6L to createLanguage(6L, "de"),
        ),
    )

    // Capture the JSONL content via argThat
    var capturedJsonl: ByteArray? = null
    whenever(
      openAiBatchApiService.submitBatch(
        apiKey = any(),
        apiUrl = any(),
        jsonlContent =
          argThat {
            capturedJsonl = this
            true
          },
        endpoint = any(),
        model = any(),
        metadata = anyOrNull(),
      ),
    ).thenReturn(BatchSubmissionResult(batchId = "batch_1", inputFileId = "file_1"))

    assertThrows<WaitingForExternalException> {
      submissionService.submitBatch(job, chunk, chunkExecution)
    }

    val jsonlContent = String(capturedJsonl!!, Charsets.UTF_8)
    val lines = jsonlContent.lines().filter { it.isNotBlank() }

    lines.assert.hasSize(2)

    // Verify first line
    val line1 = objectMapper.readTree(lines[0])
    line1
      .get("custom_id")
      .asText()
      .assert
      .isEqualTo("42:100:5")
    line1
      .get("method")
      .asText()
      .assert
      .isEqualTo("POST")
    line1
      .get("url")
      .asText()
      .assert
      .isEqualTo("/v1/chat/completions")
    line1
      .get("body")
      .get("model")
      .asText()
      .assert
      .isEqualTo("gpt-4o-mini")

    // Verify second line
    val line2 = objectMapper.readTree(lines[1])
    line2
      .get("custom_id")
      .asText()
      .assert
      .isEqualTo("42:101:6")
  }

  @Test
  fun `poller parses completed batch results and stores them on tracker`() {
    val jsonl =
      listOf(
        """{"id":"batch_req_1","custom_id":"42:100:5","response":{"status_code":200,"request_id":"req_1","body":{"id":"chatcmpl-1","object":"chat.completion","created":1700000000,"model":"gpt-4o-mini","choices":[{"index":0,"message":{"role":"assistant","content":"Ahoj svete"},"finish_reason":"stop"}],"usage":{"prompt_tokens":50,"completion_tokens":15,"total_tokens":65}}},"error":null}""",
        """{"id":"batch_req_2","custom_id":"42:101:6","response":{"status_code":200,"request_id":"req_2","body":{"id":"chatcmpl-2","object":"chat.completion","created":1700000000,"model":"gpt-4o-mini","choices":[{"index":0,"message":{"role":"assistant","content":"Hallo Welt"},"finish_reason":"stop"}],"usage":{"prompt_tokens":55,"completion_tokens":18,"total_tokens":73}}},"error":null}""",
      ).joinToString("\n")

    val results = poller.parseResults(jsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(2)

    results[0].let {
      it.customId.assert.isEqualTo("42:100:5")
      it.keyId.assert.isEqualTo(100L)
      it.languageId.assert.isEqualTo(5L)
      it.translatedText.assert.isEqualTo("Ahoj svete")
      it.promptTokens.assert.isEqualTo(50L)
      it.completionTokens.assert.isEqualTo(15L)
    }

    results[1].let {
      it.customId.assert.isEqualTo("42:101:6")
      it.keyId.assert.isEqualTo(101L)
      it.languageId.assert.isEqualTo(6L)
      it.translatedText.assert.isEqualTo("Hallo Welt")
    }
  }

  @Test
  fun `result applier saves translations and reports progress`() {
    val jobId = 42L
    val chunkExecutionId = 99L
    val keyId = 100L
    val languageId = 5L

    val job = createJobDto(projectId = 1L, jobId = jobId)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:100:5",
              keyId = keyId,
              languageId = languageId,
              translatedText = "Ahoj svete",
              contextDescription = null,
              promptTokens = 50,
              completionTokens = 15,
            ),
          )
      }

    val key = createKey(keyId, "greeting")
    val language = createLanguage(languageId, "cs")

    whenever(trackerRepository.findByChunkExecutionId(chunkExecutionId)).thenReturn(tracker)
    whenever(keyService.find(listOf(keyId))).thenReturn(listOf(key))
    whenever(languageService.findByIdIn(setOf(languageId))).thenReturn(listOf(language))

    resultApplier.applyResults(job, chunkExecutionId)

    // Verify translation was saved
    verify(translationService).setTranslationText(eq(key), eq(language), eq("Ahoj svete"))

    // Verify progress was reported
    verify(progressManager).reportSingleChunkProgress(jobId)

    // Verify tracker status updated
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `result applier skips results with errors`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:100:5",
              keyId = 100L,
              languageId = 5L,
              translatedText = null,
              contextDescription = null,
              error = "Rate limit exceeded",
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(emptyList())
    whenever(languageService.findByIdIn(any())).thenReturn(emptyList())

    resultApplier.applyResults(job, 99L)

    // Translation and progress should NOT be called for error results
    org.mockito.Mockito.verifyNoInteractions(translationService)
    org.mockito.Mockito.verifyNoInteractions(progressManager)

    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `result applier handles multiple results with mixed success and error`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)

    val key100 = createKey(100L, "hello")
    val key101 = createKey(101L, "goodbye")
    val langCs = createLanguage(5L, "cs")

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:100:5",
              keyId = 100L,
              languageId = 5L,
              translatedText = "Ahoj",
              contextDescription = null,
            ),
            OpenAiBatchResult(
              customId = "42:101:5",
              keyId = 101L,
              languageId = 5L,
              translatedText = null,
              contextDescription = null,
              error = "Internal error",
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key100, key101))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(langCs))

    resultApplier.applyResults(job, 99L)

    // Only the successful result should trigger translation
    verify(translationService).setTranslationText(eq(key100), eq(langCs), eq("Ahoj"))
    // Progress reported once (for the successful one only)
    verify(progressManager).reportSingleChunkProgress(42L)
  }

  @Test
  fun `result applier handles tracker with no results`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results = null
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)

    resultApplier.applyResults(job, 99L)

    org.mockito.Mockito.verifyNoInteractions(translationService)
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `full flow - submission creates correct custom_id format for poller to parse`() {
    val jobId = 42L
    val keyId = 100L
    val languageId = 5L
    val customId = "$jobId:$keyId:$languageId"

    val responseJsonl =
      """{"id":"batch_req_1","custom_id":"$customId","response":{"status_code":200,"request_id":"req_1","body":{"choices":[{"index":0,"message":{"role":"assistant","content":"Translated text"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}},"error":null}"""

    val results = poller.parseResults(responseJsonl.toByteArray(Charsets.UTF_8))

    results.assert.hasSize(1)
    results[0].let {
      it.customId.assert.isEqualTo(customId)
      it.keyId.assert.isEqualTo(keyId)
      it.languageId.assert.isEqualTo(languageId)
      it.translatedText.assert.isEqualTo("Translated text")
      it.error.assert.isNull()
    }
  }

  // -- Helper methods --

  private fun createJobDto(
    projectId: Long,
    jobId: Long,
  ): BatchJobDto =
    BatchJobDto(
      id = jobId,
      projectId = projectId,
      authorId = 1L,
      target = emptyList(),
      totalItems = 0,
      totalChunks = 1,
      chunkSize = 100,
      status = BatchJobStatus.RUNNING,
      type = BatchJobType.MACHINE_TRANSLATE,
      params = null,
      maxPerJobConcurrency = 1,
      jobCharacter = JobCharacter.SLOW,
      hidden = false,
      debouncingKey = null,
    )

  private fun createChunkExecution(jobId: Long): BatchJobChunkExecution {
    val batchJob = mock(BatchJob::class.java)
    whenever(batchJob.id).thenReturn(jobId)

    val execution = mock(BatchJobChunkExecution::class.java)
    whenever(execution.id).thenReturn(1L)
    whenever(execution.batchJob).thenReturn(batchJob)
    return execution
  }

  private fun setupProviderConfig(projectId: Long) {
    val projectDto = mock(io.tolgee.dtos.cacheable.ProjectDto::class.java)
    whenever(projectDto.organizationOwnerId).thenReturn(10L)
    whenever(projectService.getDto(projectId)).thenReturn(projectDto)

    val providerConfig =
      LlmProviderDto(
        id = 1L,
        name = "openai",
        type = LlmProviderType.OPENAI,
        priority = null,
        apiKey = "test-api-key",
        rawApiUrl = "https://api.openai.com",
        model = "gpt-4o-mini",
        deployment = null,
        format = null,
        reasoningEffort = null,
        tokenPriceInCreditsInput = null,
        tokenPriceInCreditsOutput = null,
        attempts = null,
        maxTokens = 4096,
        batchApiEnabled = true,
      )
    whenever(llmProviderService.getProviderByName(any(), any(), anyOrNull())).thenReturn(providerConfig)
    whenever(llmProviderService.findBatchEnabledProvider(any())).thenReturn(providerConfig)
  }

  private fun setupPromptServiceDefaults() {
    val defaultPromptDto = PromptDto(name = "default", providerName = "openai", template = "Translate: {{text}}")
    whenever(promptService.findPromptOrDefaultDto(any(), anyOrNull())).thenReturn(defaultPromptDto)
    whenever(promptService.getDefaultPrompt()).thenReturn(defaultPromptDto)
    whenever(promptService.getPrompt(any(), any(), any(), any(), anyOrNull())).thenReturn("Translate this text")

    val llmParams =
      LlmParams(
        messages =
          listOf(
            LlmParams.Companion.LlmMessage(
              type = LlmParams.Companion.LlmMessageType.TEXT,
              text = "Translate this text",
            ),
          ),
        shouldOutputJson = false,
        priority = LlmProviderPriority.LOW,
      )
    whenever(promptService.getLlmParamsFromPrompt(any(), any(), any())).thenReturn(llmParams)
  }

  private fun setupKeysAndLanguages(
    keys: Map<Long, Key>,
    languages: Map<Long, Language>,
  ) {
    whenever(keyService.find(any<List<Long>>()))
      .thenReturn(keys.values.toList())
    whenever(languageService.findByIdIn(any()))
      .thenReturn(languages.values.toList())
  }

  private fun createKey(
    id: Long,
    name: String,
  ): Key {
    val key = mock(Key::class.java)
    whenever(key.id).thenReturn(id)
    whenever(key.name).thenReturn(name)
    return key
  }

  private fun createLanguage(
    id: Long,
    tag: String,
  ): Language {
    val language = mock(Language::class.java)
    whenever(language.id).thenReturn(id)
    whenever(language.tag).thenReturn(tag)
    return language
  }
}
