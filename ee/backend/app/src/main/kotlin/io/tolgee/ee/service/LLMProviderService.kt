package io.tolgee.ee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.machineTranslation.LLMProperties
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.LLMParams
import io.tolgee.dtos.LLMProviderDto
import io.tolgee.dtos.request.llmProvider.LLMProviderRequest
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto
import io.tolgee.ee.component.llm.*
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.LLMProvider
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType
import io.tolgee.repository.LLMProviderRepository
import io.tolgee.service.PromptService
import io.tolgee.service.organization.OrganizationService
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.set
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException.TooManyRequests
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.time.Duration
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

const val TOKEN_PRICE = 0.000_035 // EUR

@Service
class LLMProviderService(
  private val llmProviderRepository: LLMProviderRepository,
  private val organizationService: OrganizationService,
  private val providerLLMProperties: LLMProperties,
  private val openaiApiService: OpenaiApiService,
  private val ollamaApiService: OllamaApiService,
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider,
  private val claudeApiService: ClaudeApiService,
  private val geminiApiService: GeminiApiService,
  private val restTemplateBuilder: RestTemplateBuilder,
  private val internalProperties: InternalProperties,
) {
  private val cache: Cache by lazy { cacheManager.getCache(Caches.LLM_PROVIDERS) }
  private var lastUsedMap: MutableMap<String, Long> = mutableMapOf()

  fun getProviderByName(
    organizationId: Long,
    name: String,
    priority: LLMProviderPriority?,
  ): LLMProviderDto {
    val customProviders = getAll(organizationId)
    val serverProviders = getAllServerProviders()
    val providersOfTheName =
      if (customProviders.find { it.name == name } != null) {
        customProviders
      } else {
        serverProviders
      }.filter {
        it.name == name
      }

    val providersWithPriority =
      if (providersOfTheName.find { it.priority == priority } != null) {
        providersOfTheName.filter { it.priority == priority }
      } else {
        providersOfTheName
      }

    if (providersWithPriority.isEmpty()) {
      throw BadRequestException(Message.LLM_PROVIDER_NOT_FOUND, listOf(name))
    }

    val providerInfo = cache.get(name, ProviderInfo::class.java)
    val providers =
      providersWithPriority.filter {
        if (providerInfo != null) {
          providerInfo.suspendMap.getOrDefault(it.id, 0L) < currentDateProvider.date.time
        } else {
          true
        }
      }
    if (providers.isEmpty() && providerInfo?.suspendMap?.isNotEmpty() != null) {
      val closestUnsuspend = providerInfo.suspendMap.map { (_, time) -> time }.min()
      throw TranslationApiRateLimitException(closestUnsuspend)
    }

    var lastUsed = lastUsedMap.get(name)
    val lastUsedIndex = providers.indexOfFirst { it.id == lastUsed }
    val newIndex = (lastUsedIndex + 1) % providers.size
    val provider = providers.get(newIndex)
    lastUsedMap.set(name, provider.id)
    return provider
  }

  fun getAll(organizationId: Long): List<LLMProviderDto> {
    return llmProviderRepository.getAll(organizationId).map { it.toDto() }
  }

  fun <T> repeatWhileProvidersRateLimited(
    organizationId: Long,
    provider: String,
    priority: LLMProviderPriority?,
    callback: (provider: LLMProviderDto) -> T,
  ): T {
    var lastError: Exception? = null

    // attempt 3 times to find non-rate-limited provider
    for (i in 0..3) {
      val providerConfig = getProviderByName(organizationId, provider, priority)
      try {
        return callback(providerConfig)
      } catch (e: TooManyRequests) {
        suspendProvider(provider, providerConfig.id, 60 * 1000)
        lastError = e
      }
    }
    throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(lastError!!.message), lastError!!)
  }

  fun <T> repeatWithTimeouts(
    attempts: List<Int>,
    callback: (restTemplate: RestTemplate) -> T,
  ): T {
    var lastError: Exception? = null
    for (timeout in attempts) {
      val restTemplate = restTemplateBuilder.setReadTimeout(Duration.ofSeconds(timeout.toLong())).build()
      try {
        return callback(restTemplate)
      } catch (e: ResourceAccessException) {
        lastError = e
      }
    }
    throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(lastError!!.message), lastError)
  }

  fun callProvider(
    organizationId: Long,
    provider: String,
    params: LLMParams,
    priority: LLMProviderPriority? = null,
  ): PromptService.Companion.PromptResult {
    return repeatWhileProvidersRateLimited(organizationId, provider, priority) { providerConfig ->
      val providerService = getProviderService(providerConfig.type)
      val attempts = providerConfig.attempts ?: providerService.defaultAttempts()
      repeatWithTimeouts(attempts) { restTemplate ->
        val result = getProviderResponse(providerService, params, providerConfig, restTemplate)
        result.price = calculatePrice(providerConfig, result.usage)
        result
      }
    }
  }

  fun getFakedResponse(
    params: LLMParams,
    config: LLMProviderInterface,
    restTemplate: RestTemplate,
  ): PromptService.Companion.PromptResult {
    val json =
      """
      {
        "output": "response from: ${config.name}",
        "contextDescription": "context description from: ${config.name}"
      }
      """.trimIndent()
    return PromptService.Companion.PromptResult(
      response = json,
      usage = PromptResponseUsageDto(inputTokens = 42, outputTokens = 42, cachedTokens = 21),
      price = 42,
    )
  }

  fun getProviderResponse(
    providerService: AbstractLLMApiService,
    params: LLMParams,
    config: LLMProviderInterface,
    restTemplate: RestTemplate,
  ): PromptService.Companion.PromptResult {
    if (internalProperties.fakeLlmProviders) {
      return getFakedResponse(params, config, restTemplate)
    }
    return providerService.translate(params, config, restTemplate)
  }

  fun getProviderService(providerType: LLMProviderType): AbstractLLMApiService {
    return when (providerType) {
      LLMProviderType.OPENAI -> openaiApiService
      LLMProviderType.OLLAMA -> ollamaApiService
      LLMProviderType.CLAUDE -> claudeApiService
      LLMProviderType.GEMINI -> geminiApiService
    }
  }

  fun suspendProvider(
    name: String,
    providerId: Long,
    period: Long,
  ) {
    val providerInfo = cache.get(name, ProviderInfo::class.java) ?: ProviderInfo()
    providerInfo.suspendMap.set(providerId, currentDateProvider.date.time + period)
    cache.set(name, providerInfo)
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
        pricePerMillionInput = null,
        pricePerMillionOutput = null,
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

  fun calculatePrice(
    providerConfig: LLMProviderDto,
    usage: PromptResponseUsageDto?,
  ): Int {
    val pricePerTokenInput: Double = (providerConfig.pricePerMillionInput ?: 0.0) / 1_000_000.0
    val pricePerTokenOutput: Double = (providerConfig.pricePerMillionOutput ?: 0.0) / 1_000_000.0
    val inputTokens: Long = usage?.inputTokens ?: 0L
    val outputTokens: Long = usage?.outputTokens ?: 0L
    val cachedTokens: Long = usage?.cachedTokens ?: 0L

    val inputPrice = (inputTokens - cachedTokens) * pricePerTokenInput
    val outputPrice = (outputTokens) * pricePerTokenOutput

    val price = inputPrice + outputPrice

    return ((price / TOKEN_PRICE) * 100).roundToInt()
  }

  companion object {
    data class ProviderInfo(
      var suspendMap: MutableMap<Long, Long> = mutableMapOf(),
    )
  }
}
