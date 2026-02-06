package io.tolgee.ee.unit.batch

import io.tolgee.batch.JobCharacter
import io.tolgee.batch.ProgressManager
import io.tolgee.batch.data.BatchJobDto
import io.tolgee.batch.data.BatchJobType
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.ee.service.batch.BatchApiResultApplier
import io.tolgee.model.Language
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.batch.OpenAiBatchJobTracker
import io.tolgee.model.batch.OpenAiBatchResult
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.key.Key
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BatchApiResultApplierTest {
  private lateinit var trackerRepository: OpenAiBatchJobTrackerRepository
  private lateinit var translationService: TranslationService
  private lateinit var progressManager: ProgressManager
  private lateinit var languageService: LanguageService
  private lateinit var keyService: KeyService
  private lateinit var mtCreditsService: MtCreditsService
  private lateinit var llmProviderService: LlmProviderService
  private lateinit var projectService: ProjectService
  private lateinit var metrics: io.tolgee.Metrics

  private lateinit var resultApplier: BatchApiResultApplier

  @BeforeEach
  fun setUp() {
    trackerRepository = mock(OpenAiBatchJobTrackerRepository::class.java)
    translationService = mock(TranslationService::class.java)
    progressManager = mock(ProgressManager::class.java)
    languageService = mock(LanguageService::class.java)
    keyService = mock(KeyService::class.java)
    mtCreditsService = mock(MtCreditsService::class.java)
    llmProviderService = mock(LlmProviderService::class.java)
    projectService = mock(ProjectService::class.java)
    metrics = mock(io.tolgee.Metrics::class.java, org.mockito.Mockito.RETURNS_DEEP_STUBS)

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
  }

  @Test
  fun `throws when no tracker found for chunk execution`() {
    val job = createJobDto(jobId = 42L)
    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(null)

    val exception =
      assertThrows<IllegalStateException> {
        resultApplier.applyResults(job, 99L)
      }
    exception.message.assert.contains("No tracker found")
    exception.message.assert.contains("99")
  }

  @Test
  fun `transitions tracker status to APPLYING then COMPLETED`() {
    val job = createJobDto(jobId = 42L)

    val key = createKey(100L)
    val language = createLanguage(5L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:100:5",
              keyId = 100L,
              languageId = 5L,
              translatedText = "Translated",
              contextDescription = null,
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(language))

    resultApplier.applyResults(job, 99L)

    // Final status should be COMPLETED
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `skips result when key is not found in database`() {
    val job = createJobDto(jobId = 42L)
    val language = createLanguage(5L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:999:5",
              keyId = 999L,
              languageId = 5L,
              translatedText = "Translated",
              contextDescription = null,
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    // Key 999 not found
    whenever(keyService.find(any<List<Long>>())).thenReturn(emptyList())
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(language))

    resultApplier.applyResults(job, 99L)

    verifyNoInteractions(translationService)
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `skips result when language is not found in database`() {
    val job = createJobDto(jobId = 42L)
    val key = createKey(100L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:100:999",
              keyId = 100L,
              languageId = 999L,
              translatedText = "Translated",
              contextDescription = null,
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key))
    // Language 999 not found
    whenever(languageService.findByIdIn(any())).thenReturn(emptyList())

    resultApplier.applyResults(job, 99L)

    verifyNoInteractions(translationService)
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `continues processing after translation exception`() {
    val job = createJobDto(jobId = 42L)

    val key100 = createKey(100L)
    val key101 = createKey(101L)
    val langCs = createLanguage(5L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results =
          listOf(
            OpenAiBatchResult(
              customId = "42:100:5",
              keyId = 100L,
              languageId = 5L,
              translatedText = "Throws",
              contextDescription = null,
            ),
            OpenAiBatchResult(
              customId = "42:101:5",
              keyId = 101L,
              languageId = 5L,
              translatedText = "Succeeds",
              contextDescription = null,
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key100, key101))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(langCs))

    // First call throws, second succeeds
    val translationMock = mock(io.tolgee.model.translation.Translation::class.java)
    doThrow(RuntimeException("DB error"))
      .whenever(translationService).setTranslationText(eq(key100), eq(langCs), eq("Throws"))
    doReturn(translationMock)
      .whenever(translationService).setTranslationText(eq(key101), eq(langCs), eq("Succeeds"))

    resultApplier.applyResults(job, 99L)

    // Second translation should still be attempted even though first failed
    verify(translationService).setTranslationText(eq(key101), eq(langCs), eq("Succeeds"))
    // Progress should be reported for second (successful) item only
    verify(progressManager).reportSingleChunkProgress(42L)
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `handles multiple results for different languages`() {
    val job = createJobDto(jobId = 42L)

    val key = createKey(100L)
    val langCs = createLanguage(5L)
    val langDe = createLanguage(6L)

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
              customId = "42:100:6",
              keyId = 100L,
              languageId = 6L,
              translatedText = "Hallo",
              contextDescription = null,
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(listOf(key))
    whenever(languageService.findByIdIn(any())).thenReturn(listOf(langCs, langDe))

    resultApplier.applyResults(job, 99L)

    verify(translationService).setTranslationText(eq(key), eq(langCs), eq("Ahoj"))
    verify(translationService).setTranslationText(eq(key), eq(langDe), eq("Hallo"))
  }

  @Test
  fun `handles empty results list`() {
    val job = createJobDto(jobId = 42L)

    val tracker =
      OpenAiBatchJobTracker().apply {
        status = OpenAiBatchTrackerStatus.RESULTS_READY
        results = emptyList()
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(emptyList())
    whenever(languageService.findByIdIn(any())).thenReturn(emptyList())

    resultApplier.applyResults(job, 99L)

    verifyNoInteractions(translationService)
    verifyNoInteractions(progressManager)
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  @Test
  fun `all error results produce no translations`() {
    val job = createJobDto(jobId = 42L)

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
              error = "Rate limit",
            ),
            OpenAiBatchResult(
              customId = "42:101:5",
              keyId = 101L,
              languageId = 5L,
              translatedText = null,
              contextDescription = null,
              error = "Timeout",
            ),
          )
      }

    whenever(trackerRepository.findByChunkExecutionId(99L)).thenReturn(tracker)
    whenever(keyService.find(any<List<Long>>())).thenReturn(emptyList())
    whenever(languageService.findByIdIn(any())).thenReturn(emptyList())

    resultApplier.applyResults(job, 99L)

    verifyNoInteractions(translationService)
    verifyNoInteractions(progressManager)
    tracker.status.assert.isEqualTo(OpenAiBatchTrackerStatus.COMPLETED)
  }

  // -- Helpers --

  private fun createJobDto(jobId: Long): BatchJobDto =
    BatchJobDto(
      id = jobId,
      projectId = 1L,
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

  private fun createKey(id: Long): Key {
    val key = mock(Key::class.java)
    whenever(key.id).thenReturn(id)
    whenever(key.name).thenReturn("key_$id")
    return key
  }

  private fun createLanguage(id: Long): Language {
    val language = mock(Language::class.java)
    whenever(language.id).thenReturn(id)
    whenever(language.tag).thenReturn("lang_$id")
    return language
  }
}
