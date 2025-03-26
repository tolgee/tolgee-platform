package io.tolgee.service

import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.dtos.request.llmProvider.LLMProviderCreateDto
import io.tolgee.dtos.request.llmProvider.LLMProviderUpdateDto
import io.tolgee.model.LLMProvider
import io.tolgee.repository.LLMProviderRepository
import io.tolgee.service.organization.OrganizationService
import org.springframework.stereotype.Service

@Service
class LLMProviderService(
  private val llmProviderRepository: LLMProviderRepository,
  private val organizationService: OrganizationService,
  private val providerLLMProperties: LLMProperties,
) {
  fun getProviderByName(
    organizationId: Long,
    name: String,
  ): LLMProviderInterface? {
    return getAll(organizationId).find { it.name == name } ?: getAllServerProviders().find { it.name == name }
  }

  fun getAll(organizationId: Long): List<LLMProvider> {
    return llmProviderRepository.getAll(organizationId)
  }

  fun createProvider(
    organizationId: Long,
    dto: LLMProviderCreateDto,
  ): LLMProvider {
    val provider =
      LLMProvider(
        name = dto.name,
        type = dto.type,
        priority = dto.priority,
        apiKey = dto.apiKey,
        apiUrl = dto.apiUrl,
        model = dto.model,
        deployment = dto.deployment,
        keepAlive = dto.keepAlive,
        format = dto.format,
        organization = organizationService.get(organizationId),
      )
    llmProviderRepository.save(provider)
    return provider
  }

  fun updateProvider(
    organizationId: Long,
    providerId: Long,
    dto: LLMProviderUpdateDto,
  ): LLMProvider {
    val provider =
      LLMProvider(
        name = dto.name,
        type = dto.type,
        priority = dto.priority,
        apiKey = dto.apiKey,
        apiUrl = dto.apiUrl,
        model = dto.model,
        deployment = dto.deployment,
        keepAlive = dto.keepAlive,
        format = dto.format,
        organization = organizationService.get(organizationId),
      )
    llmProviderRepository.save(provider)
    return provider
  }

  fun deleteProvider(
    organizationId: Long,
    providerId: Long,
  ) {
    llmProviderRepository.deleteById(providerId)
  }

  fun getAllServerProviders(): List<LLMProperties.LLMProvider> {
    return providerLLMProperties.providers
  }
}
