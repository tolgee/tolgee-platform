package io.tolgee.ee.service.batch

import io.tolgee.configuration.tolgee.BatchProperties
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.batch.OpenAiBatchTrackerStatus
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.repository.batch.OpenAiBatchJobTrackerRepository
import org.springframework.stereotype.Service

@Service
class BatchApiValidationService(
  private val batchProperties: BatchProperties,
  private val openAiBatchJobTrackerRepository: OpenAiBatchJobTrackerRepository,
) {
  private val activeStatuses =
    listOf(
      OpenAiBatchTrackerStatus.SUBMITTED,
      OpenAiBatchTrackerStatus.IN_PROGRESS,
      OpenAiBatchTrackerStatus.RESULTS_READY,
      OpenAiBatchTrackerStatus.APPLYING,
    )

  fun validateBatchSubmission(
    itemCount: Int,
    organizationId: Long,
    providerType: LlmProviderType,
    batchApiEnabled: Boolean?,
  ) {
    validateProviderType(providerType)
    validateBatchApiEnabled(batchApiEnabled)
    validateItemCount(itemCount)
    validateConcurrentPerOrg(organizationId)
    validateConcurrentGlobal()
  }

  private fun validateProviderType(providerType: LlmProviderType) {
    if (providerType != LlmProviderType.OPENAI && providerType != LlmProviderType.OPENAI_AZURE) {
      throw BadRequestException(Message.BATCH_API_PROVIDER_NOT_SUPPORTED)
    }
  }

  private fun validateBatchApiEnabled(batchApiEnabled: Boolean?) {
    if (batchApiEnabled != true) {
      throw BadRequestException(Message.BATCH_API_NOT_ENABLED_FOR_PROVIDER)
    }
  }

  private fun validateItemCount(itemCount: Int) {
    if (itemCount > batchProperties.batchApiMaxItemsPerJob) {
      throw BadRequestException(Message.BATCH_API_TOO_MANY_ITEMS)
    }
  }

  private fun validateConcurrentPerOrg(organizationId: Long) {
    val activeCount =
      openAiBatchJobTrackerRepository.countByStatusInAndOrganizationId(activeStatuses, organizationId)
    if (activeCount >= batchProperties.batchApiMaxConcurrentPerOrg) {
      throw BadRequestException(Message.BATCH_API_MAX_CONCURRENT_PER_ORG_EXCEEDED)
    }
  }

  private fun validateConcurrentGlobal() {
    val activeCount = openAiBatchJobTrackerRepository.countByStatusIn(activeStatuses)
    if (activeCount >= batchProperties.batchApiMaxConcurrentGlobal) {
      throw BadRequestException(Message.BATCH_API_MAX_CONCURRENT_GLOBAL_EXCEEDED)
    }
  }
}
