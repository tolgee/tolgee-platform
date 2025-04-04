package io.tolgee.ee.service

import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.LLMParams
import io.tolgee.dtos.LLMProviderDto
import io.tolgee.dtos.request.llmProvider.LLMProviderRequest
import io.tolgee.ee.component.llm.OllamaApiService
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.LLMProvider
import io.tolgee.model.enums.LLMProviderType
import io.tolgee.repository.LLMProviderRepository
import io.tolgee.service.organization.OrganizationService
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class LLMProviderService(
  private val llmProviderRepository: LLMProviderRepository,
  private val organizationService: OrganizationService,
  private val providerLLMProperties: LLMProperties,
  private val openaiApiService: OpenaiApiService,
  private val ollamaApiService: OllamaApiService,
) {
  fun getProviderByName(
    organizationId: Long,
    name: String,
  ): LLMProviderDto? {
    return getAll(organizationId).find { it.name == name } ?: getAllServerProviders().find { it.name == name }
  }

  fun getAll(organizationId: Long): List<LLMProviderDto> {
    return llmProviderRepository.getAll(organizationId).map { it.toDto() }
  }

  fun callProvider(
    organizationId: Long,
    provider: String,
    params: LLMParams,
  ): MtValueProvider.MtResult {
    val providerConfig =
      getProviderByName(organizationId, provider)
        ?: throw BadRequestException(Message.LLM_PROVIDER_NOT_FOUND, listOf(provider))
    return when (providerConfig.type) {
      LLMProviderType.OPENAI -> openaiApiService.translate(params, providerConfig)
      LLMProviderType.OLLAMA -> ollamaApiService.translate(params, providerConfig)
    }
  }

  fun createProvider(
    organizationId: Long,
    dto: LLMProviderRequest,
  ): LLMProviderDto {
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
    return provider.toDto()
  }

  fun updateProvider(
    organizationId: Long,
    providerId: Long,
    dto: LLMProviderRequest,
  ): LLMProviderDto {
    val provider = llmProviderRepository.findById(providerId).getOrNull() ?: throw NotFoundException()
    provider.name = dto.name
    provider.type = dto.type
    provider.priority = dto.priority
    provider.apiKey = dto.apiKey
    provider.apiUrl = dto.apiUrl
    provider.model = dto.model
    provider.deployment = dto.deployment
    provider.keepAlive = dto.keepAlive
    provider.format = dto.format
    provider.organization = organizationService.get(organizationId)
    llmProviderRepository.save(provider)
    return provider.toDto()
  }

  fun deleteProvider(
    organizationId: Long,
    providerId: Long,
  ) {
    llmProviderRepository.deleteById(providerId)
  }

  fun getAllServerProviders(): List<LLMProviderDto> {
    return providerLLMProperties.providers.mapIndexed { index, llmProvider ->
      // server configured providers are indexed like -1, -2, -3, to identify them
      llmProvider.toDto(-(index.toLong()) - 1)
    }
  }
}
