package io.tolgee.ee.service

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.BatchTranslateInfoDto
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.dtos.PromptResult
import io.tolgee.dtos.request.llmProvider.LlmProviderRequest
import io.tolgee.ee.component.llm.AbstractLlmApiService
import io.tolgee.ee.component.llm.AnthropicApiService
import io.tolgee.ee.component.llm.GoogleAiApiService
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.ee.component.llm.TolgeeApiService
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.exceptions.InvalidStateException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.LlmProvider
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.repository.LlmProviderRepository
import io.tolgee.service.LlmPropertiesService
import io.tolgee.service.organization.OrganizationService
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.set
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.time.Duration
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt

@Service
class LlmProviderService(
  private val llmProviderRepository: LlmProviderRepository,
  private val organizationService: OrganizationService,
  private val llmPropertiesService: LlmPropertiesService,
  private val openaiApiService: OpenaiApiService,
  private val tolgeeApiService: TolgeeApiService,
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider,
  private val restTemplateBuilder: RestTemplateBuilder,
  private val internalProperties: InternalProperties,
  private val anthropicApiService: AnthropicApiService,
  private val googleAiApiService: GoogleAiApiService,
) {
  private val cache: Cache by lazy { cacheManager.getCache(Caches.LLM_PROVIDERS) ?: throw InvalidStateException() }
  private var lastUsedMap: MutableMap<String, Long> = mutableMapOf()

  fun callProvider(
    organizationId: Long,
    provider: String,
    params: LlmParams,
    attempts: List<Int>? = null,
  ): PromptResult {
    val result =
      repeatWhileProvidersRateLimited(organizationId, provider, params.priority) { providerConfig ->
        val providerService = getProviderService(providerConfig.type)
        val resolvedAttempts = attempts ?: providerConfig.attempts ?: providerService.defaultAttempts()
        repeatWithTimeouts(resolvedAttempts) { restTemplate ->
          val result = getProviderResponse(providerService, params, providerConfig, restTemplate)
          result.price = if (result.price != 0) result.price else calculatePrice(providerConfig, result.usage)
          result
        }
      }
    return result
  }

  fun createProvider(
    organizationId: Long,
    dto: LlmProviderRequest,
  ): LlmProviderDto {
    val provider =
      LlmProvider(
        name = dto.name,
        type = dto.type,
        priority = dto.priority,
        apiKey = dto.apiKey,
        apiUrl = dto.apiUrl,
        model = dto.model,
        deployment = dto.deployment,
        keepAlive = dto.keepAlive,
        format = dto.format,
        reasoningEffort = dto.reasoningEffort,
        batchApiEnabled = dto.batchApiEnabled,
        organization = organizationService.get(organizationId),
      )
    llmProviderRepository.save(provider)
    return provider.toDto()
  }

  @Transactional
  fun updateProvider(
    organizationId: Long,
    providerId: Long,
    dto: LlmProviderRequest,
  ): LlmProviderDto {
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
    provider.reasoningEffort = dto.reasoningEffort
    provider.batchApiEnabled = dto.batchApiEnabled
    provider.organization = organizationService.get(organizationId)
    llmProviderRepository.save(provider)
    return provider.toDto()
  }

  @Transactional
  fun deleteProvider(
    organizationId: Long,
    providerId: Long,
  ) {
    llmProviderRepository.deleteByIdAndOrganizationId(providerId, organizationId)
  }

  fun getAll(organizationId: Long): List<LlmProviderDto> {
    return llmProviderRepository.getAll(organizationId).map { it.toDto() }
  }

  fun getAllServerProviders(): List<LlmProviderDto> {
    return llmPropertiesService.getProviders().mapIndexed { index, llmProvider ->
      // server configured providers are indexed like -1, -2, -3, to identify them
      llmProvider.toDto(-(index.toLong()) - 1)
    }
  }

  fun <T> repeatWhileProvidersRateLimited(
    organizationId: Long,
    provider: String,
    priority: LlmProviderPriority?,
    callback: (provider: LlmProviderDto) -> T,
  ): T {
    var lastError: Exception? = null
    var retryAt: Long? = null

    repeat(3) {
      val providerConfig = getProviderByName(organizationId, provider, priority)
      try {
        return callback(providerConfig)
      } catch (e: LlmRateLimitedException) {
        retryAt = e.retryAt
        lastError = e
      } catch (e: HttpClientErrorException) {
        if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
          suspendProvider(provider, providerConfig.id)
          lastError = e
        } else {
          throw e
        }
      }
    }
    throw LlmRateLimitedException(
      retryAt = retryAt ?: (currentDateProvider.date.time + RATE_LIMIT_SUSPEND_PERIOD_MS),
      params = lastError?.message?.let { listOf(it) },
      cause = lastError,
    )
  }

  fun <T> repeatWithTimeouts(
    attempts: List<Int>,
    callback: (restTemplate: RestTemplate) -> T,
  ): T {
    var lastError: Exception? = null
    for (timeout in attempts) {
      val restTemplate = restTemplateBuilder.readTimeout(Duration.ofSeconds(timeout.toLong())).build()
      try {
        return callback(restTemplate)
      } catch (e: ResourceAccessException) {
        lastError = e
      }
    }
    throw FailedDependencyException(Message.LLM_PROVIDER_ERROR, listOf(lastError!!.message), lastError)
  }

  fun getProviderByName(
    organizationId: Long,
    name: String,
    priority: LlmProviderPriority?,
  ): LlmProviderDto {
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

    if (providers.isEmpty() && providerInfo?.suspendMap?.isNotEmpty() == true) {
      val closestUnsuspend = providerInfo.suspendMap.map { (_, time) -> time }.min()
      throw LlmRateLimitedException(closestUnsuspend)
    }

    var lastUsed = lastUsedMap.get(name)
    val lastUsedIndex = providers.indexOfFirst { it.id == lastUsed }
    val newIndex = (lastUsedIndex + 1) % providers.size
    val provider = providers.get(newIndex)
    lastUsedMap.set(name, provider.id)
    return provider
  }

  fun getProviderResponse(
    providerService: AbstractLlmApiService,
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    if (internalProperties.fakeLlmProviders) {
      return getFakedResponse(config)
    }
    return providerService.translate(params, config, restTemplate)
  }

  fun getProviderService(providerType: LlmProviderType): AbstractLlmApiService {
    return when (providerType) {
      LlmProviderType.OPENAI -> openaiApiService
      LlmProviderType.OPENAI_AZURE -> openaiApiService
      LlmProviderType.TOLGEE -> tolgeeApiService
      LlmProviderType.ANTHROPIC -> anthropicApiService
      LlmProviderType.GOOGLE_AI -> googleAiApiService
    }
  }

  fun calculatePrice(
    providerConfig: LlmProviderDto,
    usage: PromptResult.Usage?,
    isBatchApi: Boolean = false,
  ): Int {
    val tokenPriceInCreditsInput: Double
    val tokenPriceInCreditsOutput: Double

    if (isBatchApi) {
      tokenPriceInCreditsInput = providerConfig.batchTokenPriceInCreditsInput
        ?: providerConfig.tokenPriceInCreditsInput
        ?: 0.0
      tokenPriceInCreditsOutput = providerConfig.batchTokenPriceInCreditsOutput
        ?: providerConfig.tokenPriceInCreditsOutput
        ?: 0.0
    } else {
      tokenPriceInCreditsInput = providerConfig.tokenPriceInCreditsInput ?: 0.0
      tokenPriceInCreditsOutput = providerConfig.tokenPriceInCreditsOutput ?: 0.0
    }

    val inputTokens: Long = usage?.inputTokens ?: 0L
    val outputTokens: Long = usage?.outputTokens ?: 0L
    val cachedTokens: Long = usage?.cachedTokens ?: 0L

    val inputPrice = (inputTokens - cachedTokens) * tokenPriceInCreditsInput
    val outputPrice = (outputTokens) * tokenPriceInCreditsOutput

    val price = inputPrice + outputPrice

    return (price * 100).roundToInt()
  }

  fun calculateBatchDiscountPercent(providerConfig: LlmProviderDto): Double? {
    val syncInput = providerConfig.tokenPriceInCreditsInput ?: return null
    val syncOutput = providerConfig.tokenPriceInCreditsOutput ?: return null
    val batchInput = providerConfig.batchTokenPriceInCreditsInput ?: return null
    val batchOutput = providerConfig.batchTokenPriceInCreditsOutput ?: return null

    // Weighted average discount using typical 1:3 input:output ratio
    val syncWeighted = syncInput + (syncOutput * 3)
    val batchWeighted = batchInput + (batchOutput * 3)

    if (syncWeighted <= 0.0) return null

    return ((syncWeighted - batchWeighted) / syncWeighted * 100)
      .coerceIn(0.0, 100.0)
  }

  fun findBatchEnabledProvider(organizationId: Long): LlmProviderDto? {
    val batchSupportedTypes = setOf(LlmProviderType.OPENAI, LlmProviderType.OPENAI_AZURE)
    return getMergedProviders(organizationId).find {
      it.batchApiEnabled == true && it.type in batchSupportedTypes
    }
  }

  fun getProviderById(
    organizationId: Long,
    providerId: Long,
  ): LlmProviderDto? {
    return getMergedProviders(organizationId).find { it.id == providerId }
  }

  fun getBatchInfo(organizationId: Long): BatchTranslateInfoDto {
    val batchProvider = findBatchEnabledProvider(organizationId)

    if (batchProvider == null) {
      return BatchTranslateInfoDto(
        available = false,
        discountPercent = null,
        userChoiceAllowed = false,
        providerType = null,
      )
    }

    return BatchTranslateInfoDto(
      available = true,
      discountPercent = calculateBatchDiscountPercent(batchProvider),
      userChoiceAllowed = true,
      providerType = batchProvider.type,
    )
  }

  private fun getMergedProviders(organizationId: Long): List<LlmProviderDto> {
    val customProviders = getAll(organizationId)
    val serverProviders = getAllServerProviders()

    val existing = mutableSetOf<String>()
    val result = mutableListOf<LlmProviderDto>()
    customProviders.forEach {
      if (existing.add(it.name)) {
        result.add(it)
      }
    }
    serverProviders.forEach {
      if (existing.add(it.name)) {
        result.add(it)
      }
    }
    return result
  }

  fun suspendProvider(
    name: String,
    providerId: Long,
  ) {
    val providerInfo = cache.get(name, ProviderInfo::class.java) ?: ProviderInfo()
    providerInfo.suspendMap.set(providerId, currentDateProvider.date.time + RATE_LIMIT_SUSPEND_PERIOD_MS)
    cache.set(name, providerInfo)
  }

  fun getFakedResponse(config: LlmProviderInterface): PromptResult {
    val json =
      """
      {
        "output": "response from: ${config.name}",
        "contextDescription": "context description from: ${config.name}"
      }
      """.trimIndent()
    return PromptResult(
      response = json,
      usage = PromptResult.Usage(inputTokens = 42, outputTokens = 21, cachedTokens = 1),
    )
  }

  companion object {
    private const val RATE_LIMIT_SUSPEND_PERIOD_MS = 60000L

    data class ProviderInfo(
      var suspendMap: MutableMap<Long, Long> = mutableMapOf(),
    )
  }
}
