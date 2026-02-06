package io.tolgee.ee.unit.batch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.batch.JobCharacter
import io.tolgee.batch.WaitingForExternalException
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.ee.service.BatchSubmissionResult
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.ee.service.OpenAiBatchApiService
import io.tolgee.ee.service.batch.BatchApiSubmissionService
import io.tolgee.ee.service.batch.BatchApiValidationService
import io.tolgee.model.Language
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import io.tolgee.service.project.ProjectService
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
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.whenever

class BatchApiSubmissionServiceTest {
  private val objectMapper = jacksonObjectMapper()

  private lateinit var openAiBatchApiService: OpenAiBatchApiService
  private lateinit var trackerRepository: OpenAiBatchJobTrackerRepository
  private lateinit var entityManager: EntityManager
  private lateinit var llmProviderService: LlmProviderService
  private lateinit var projectService: ProjectService
  private lateinit var keyService: KeyService
  private lateinit var languageService: LanguageService
  private lateinit var mtCreditsService: MtCreditsService
  private lateinit var batchApiValidationService: BatchApiValidationService
  private lateinit var metrics: io.tolgee.Metrics
  private lateinit var promptService: PromptServiceEeImpl

  private lateinit var submissionService: BatchApiSubmissionService

  @BeforeEach
  fun setUp() {
    openAiBatchApiService = mock(OpenAiBatchApiService::class.java)
    trackerRepository = mock(OpenAiBatchJobTrackerRepository::class.java)
    entityManager = mock(EntityManager::class.java)
    llmProviderService = mock(LlmProviderService::class.java)
    projectService = mock(ProjectService::class.java)
    keyService = mock(KeyService::class.java)
    languageService = mock(LanguageService::class.java)
    mtCreditsService = mock(MtCreditsService::class.java)
    batchApiValidationService = mock(BatchApiValidationService::class.java)
    metrics = mock(io.tolgee.Metrics::class.java, org.mockito.Mockito.RETURNS_DEEP_STUBS)
    promptService = mock(PromptServiceEeImpl::class.java)

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

  @Test
  fun `throws when project ID is null`() {
    val job = createJobDto(projectId = null, jobId = 1L)
    val chunk = listOf(BatchTranslationTargetItem(100L, 5L))
    val chunkExecution = createChunkExecution(jobId = 1L)

    val exception =
      assertThrows<IllegalStateException> {
        submissionService.submitBatch(job, chunk, chunkExecution)
      }
    exception.message.assert.contains("Project ID is required")
    verifyNoInteractions(openAiBatchApiService)
  }

  @Test
  fun `throws when API key is missing`() {
    val job = createJobDto(projectId = 1L, jobId = 1L)
    val chunk = listOf(BatchTranslationTargetItem(100L, 5L))
    val chunkExecution = createChunkExecution(jobId = 1L)

    // Setup provider config with null API key -- must also mock findBatchEnabledProvider
    setupProviderConfig(projectId = 1L, apiKey = null)

    val exception =
      assertThrows<IllegalStateException> {
        submissionService.submitBatch(job, chunk, chunkExecution)
      }
    exception.message.assert.contains("API key is required")
    verifyNoInteractions(openAiBatchApiService)
  }

  @Test
  fun `skips items with missing keys in JSONL`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)
    val chunk =
      listOf(
        BatchTranslationTargetItem(100L, 5L),
        BatchTranslationTargetItem(999L, 5L),
      )
    val chunkExecution = createChunkExecution(jobId = 42L)

    setupProviderConfig(projectId = 1L)

    // Override prompt service to throw for missing key 999
    whenever(promptService.getPrompt(any(), any(), any(), any(), anyOrNull())).thenAnswer { invocation ->
      val keyId = invocation.getArgument<Long>(2)
      if (keyId == 999L) {
        throw io.tolgee.exceptions.NotFoundException(io.tolgee.constants.Message.KEY_NOT_FOUND)
      }
      "Translate this text"
    }

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

    // Only key 100 should be present; key 999 failed prompt construction
    lines.assert.hasSize(1)
    val line = objectMapper.readTree(lines[0])
    line
      .get("custom_id")
      .asText()
      .assert
      .isEqualTo("42:100:5")
  }

  @Test
  fun `skips items with missing languages in JSONL`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)
    val chunk =
      listOf(
        BatchTranslationTargetItem(100L, 5L),
        BatchTranslationTargetItem(100L, 999L),
      )
    val chunkExecution = createChunkExecution(jobId = 42L)

    setupProviderConfig(projectId = 1L)

    // Override prompt service to throw for missing language 999
    whenever(promptService.getPrompt(any(), any(), any(), any(), anyOrNull())).thenAnswer { invocation ->
      val targetLanguageId = invocation.getArgument<Long>(3)
      if (targetLanguageId == 999L) {
        throw io.tolgee.exceptions.NotFoundException(io.tolgee.constants.Message.LANGUAGE_NOT_FOUND)
      }
      "Translate this text"
    }

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

    // Only language 5 should be present; language 999 failed prompt construction
    lines.assert.hasSize(1)
    val line = objectMapper.readTree(lines[0])
    line
      .get("custom_id")
      .asText()
      .assert
      .isEqualTo("42:100:5")
  }

  @Test
  fun `uses default API URL when not configured`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)
    val chunk = listOf(BatchTranslationTargetItem(100L, 5L))
    val chunkExecution = createChunkExecution(jobId = 42L)

    // Setup provider with null apiUrl
    setupProviderConfig(projectId = 1L, apiUrl = null)

    val key100 = mock(io.tolgee.model.key.Key::class.java)
    whenever(key100.id).thenReturn(100L)
    whenever(key100.name).thenReturn("hello")
    val langCs = mock(Language::class.java)
    whenever(langCs.id).thenReturn(5L)
    whenever(langCs.tag).thenReturn("cs")

    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key100))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(langCs))

    var capturedApiUrl: String? = null
    whenever(
      openAiBatchApiService.submitBatch(
        apiKey = any(),
        apiUrl =
          argThat {
            capturedApiUrl = this
            true
          },
        jsonlContent = any(),
        endpoint = any(),
        model = any(),
        metadata = anyOrNull(),
      ),
    ).thenReturn(BatchSubmissionResult(batchId = "batch_1", inputFileId = "file_1"))

    assertThrows<WaitingForExternalException> {
      submissionService.submitBatch(job, chunk, chunkExecution)
    }

    capturedApiUrl.assert.isEqualTo("https://api.openai.com")
  }

  @Test
  fun `uses default model when not configured`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)
    val chunk = listOf(BatchTranslationTargetItem(100L, 5L))
    val chunkExecution = createChunkExecution(jobId = 42L)

    setupProviderConfig(projectId = 1L, model = null)

    val key100 = mock(io.tolgee.model.key.Key::class.java)
    whenever(key100.id).thenReturn(100L)
    whenever(key100.name).thenReturn("hello")
    val langCs = mock(Language::class.java)
    whenever(langCs.id).thenReturn(5L)
    whenever(langCs.tag).thenReturn("cs")

    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key100))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(langCs))

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
    val line = objectMapper.readTree(jsonlContent.lines().first { it.isNotBlank() })
    line
      .get("body")
      .get("model")
      .asText()
      .assert
      .isEqualTo("gpt-4o-mini")
  }

  @Test
  fun `tracker records provider ID from config`() {
    val job = createJobDto(projectId = 1L, jobId = 42L)
    val chunk = listOf(BatchTranslationTargetItem(100L, 5L))
    val chunkExecution = createChunkExecution(jobId = 42L)

    setupProviderConfig(projectId = 1L, providerId = 77L)

    val key100 = mock(io.tolgee.model.key.Key::class.java)
    whenever(key100.id).thenReturn(100L)
    whenever(key100.name).thenReturn("hello")
    val langCs = mock(Language::class.java)
    whenever(langCs.id).thenReturn(5L)
    whenever(langCs.tag).thenReturn("cs")

    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key100))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(langCs))

    whenever(
      openAiBatchApiService.submitBatch(
        apiKey = any(),
        apiUrl = any(),
        jsonlContent = any(),
        endpoint = any(),
        model = any(),
        metadata = anyOrNull(),
      ),
    ).thenReturn(BatchSubmissionResult(batchId = "batch_1", inputFileId = "file_1"))

    assertThrows<WaitingForExternalException> {
      submissionService.submitBatch(job, chunk, chunkExecution)
    }

    org.mockito.kotlin.verify(trackerRepository).save(
      argThat {
        providerId == 77L
      },
    )
  }

  // -- Helpers --

  private fun createJobDto(
    projectId: Long?,
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

  private fun setupProviderConfig(
    projectId: Long,
    apiKey: String? = "test-api-key",
    apiUrl: String? = "https://api.openai.com",
    model: String? = "gpt-4o-mini",
    providerId: Long = 1L,
  ) {
    val projectDto = mock(io.tolgee.dtos.cacheable.ProjectDto::class.java)
    whenever(projectDto.organizationOwnerId).thenReturn(10L)
    whenever(projectService.getDto(projectId)).thenReturn(projectDto)

    val providerConfig =
      LlmProviderDto(
        id = providerId,
        name = "openai",
        type = LlmProviderType.OPENAI,
        priority = null,
        apiKey = apiKey,
        rawApiUrl = apiUrl,
        model = model,
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
}
