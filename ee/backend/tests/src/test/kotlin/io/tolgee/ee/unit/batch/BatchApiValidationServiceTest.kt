package io.tolgee.ee.unit.batch

import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.constants.Message
import io.tolgee.ee.service.batch.BatchApiValidationService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BatchApiValidationServiceTest {
  private lateinit var batchProperties: BatchProperties
  private lateinit var trackerRepository: OpenAiBatchJobTrackerRepository
  private lateinit var validationService: BatchApiValidationService

  @BeforeEach
  fun setUp() {
    batchProperties = BatchProperties()
    trackerRepository = mock()
    validationService = BatchApiValidationService(batchProperties, trackerRepository)
  }

  @Test
  fun `accepts valid OPENAI provider`() {
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(0)
    whenever(trackerRepository.countByStatusIn(any())).thenReturn(0)

    validationService.validateBatchSubmission(
      itemCount = 10,
      organizationId = 1L,
      providerType = LlmProviderType.OPENAI,
      batchApiEnabled = true,
    )
  }

  @Test
  fun `accepts valid OPENAI_AZURE provider`() {
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(0)
    whenever(trackerRepository.countByStatusIn(any())).thenReturn(0)

    validationService.validateBatchSubmission(
      itemCount = 10,
      organizationId = 1L,
      providerType = LlmProviderType.OPENAI_AZURE,
      batchApiEnabled = true,
    )
  }

  @Test
  fun `rejects unsupported provider type`() {
    val exception =
      assertThrows<BadRequestException> {
        validationService.validateBatchSubmission(
          itemCount = 10,
          organizationId = 1L,
          providerType = LlmProviderType.ANTHROPIC,
          batchApiEnabled = true,
        )
      }
    exception.tolgeeMessage.assert.isEqualTo(Message.BATCH_API_PROVIDER_NOT_SUPPORTED)
  }

  @Test
  fun `rejects when batch API is not enabled`() {
    val exception =
      assertThrows<BadRequestException> {
        validationService.validateBatchSubmission(
          itemCount = 10,
          organizationId = 1L,
          providerType = LlmProviderType.OPENAI,
          batchApiEnabled = false,
        )
      }
    exception.tolgeeMessage.assert.isEqualTo(Message.BATCH_API_NOT_ENABLED_FOR_PROVIDER)
  }

  @Test
  fun `rejects when batch API enabled is null`() {
    val exception =
      assertThrows<BadRequestException> {
        validationService.validateBatchSubmission(
          itemCount = 10,
          organizationId = 1L,
          providerType = LlmProviderType.OPENAI,
          batchApiEnabled = null,
        )
      }
    exception.tolgeeMessage.assert.isEqualTo(Message.BATCH_API_NOT_ENABLED_FOR_PROVIDER)
  }

  @Test
  fun `rejects too many items`() {
    batchProperties.batchApiMaxItemsPerJob = 100

    val exception =
      assertThrows<BadRequestException> {
        validationService.validateBatchSubmission(
          itemCount = 101,
          organizationId = 1L,
          providerType = LlmProviderType.OPENAI,
          batchApiEnabled = true,
        )
      }
    exception.tolgeeMessage.assert.isEqualTo(Message.BATCH_API_TOO_MANY_ITEMS)
  }

  @Test
  fun `accepts exactly max items`() {
    batchProperties.batchApiMaxItemsPerJob = 100
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(0)
    whenever(trackerRepository.countByStatusIn(any())).thenReturn(0)

    validationService.validateBatchSubmission(
      itemCount = 100,
      organizationId = 1L,
      providerType = LlmProviderType.OPENAI,
      batchApiEnabled = true,
    )
  }

  @Test
  fun `rejects when per-org concurrent limit exceeded`() {
    batchProperties.batchApiMaxConcurrentPerOrg = 5
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(5)

    val exception =
      assertThrows<BadRequestException> {
        validationService.validateBatchSubmission(
          itemCount = 10,
          organizationId = 1L,
          providerType = LlmProviderType.OPENAI,
          batchApiEnabled = true,
        )
      }
    exception.tolgeeMessage.assert.isEqualTo(Message.BATCH_API_MAX_CONCURRENT_PER_ORG_EXCEEDED)
  }

  @Test
  fun `accepts when per-org concurrent count is below limit`() {
    batchProperties.batchApiMaxConcurrentPerOrg = 5
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(4)
    whenever(trackerRepository.countByStatusIn(any())).thenReturn(0)

    validationService.validateBatchSubmission(
      itemCount = 10,
      organizationId = 1L,
      providerType = LlmProviderType.OPENAI,
      batchApiEnabled = true,
    )
  }

  @Test
  fun `rejects when global concurrent limit exceeded`() {
    batchProperties.batchApiMaxConcurrentGlobal = 100
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(0)
    whenever(trackerRepository.countByStatusIn(any())).thenReturn(100)

    val exception =
      assertThrows<BadRequestException> {
        validationService.validateBatchSubmission(
          itemCount = 10,
          organizationId = 1L,
          providerType = LlmProviderType.OPENAI,
          batchApiEnabled = true,
        )
      }
    exception.tolgeeMessage.assert.isEqualTo(Message.BATCH_API_MAX_CONCURRENT_GLOBAL_EXCEEDED)
  }

  @Test
  fun `accepts when global concurrent count is below limit`() {
    batchProperties.batchApiMaxConcurrentGlobal = 100
    whenever(trackerRepository.countByStatusInAndOrganizationId(any(), any())).thenReturn(0)
    whenever(trackerRepository.countByStatusIn(any())).thenReturn(99)

    validationService.validateBatchSubmission(
      itemCount = 10,
      organizationId = 1L,
      providerType = LlmProviderType.OPENAI,
      batchApiEnabled = true,
    )
  }
}
