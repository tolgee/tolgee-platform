package io.tolgee.ee.unit

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Caches
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.component.llm.AnthropicApiService
import io.tolgee.ee.component.llm.GoogleAiApiService
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.ee.component.llm.TolgeeApiService
import io.tolgee.ee.service.LlmProviderResolver
import io.tolgee.ee.service.LlmProviderService
import io.tolgee.exceptions.LlmProviderNotFoundException
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.repository.LlmProviderRepository
import io.tolgee.service.LlmPropertiesService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.web.client.RestTemplate
import java.util.Date

class LlmProviderFallbackTest {
  private lateinit var service: LlmProviderService
  private lateinit var llmPropertiesService: LlmPropertiesService
  private lateinit var llmProviderRepository: LlmProviderRepository
  private lateinit var cacheManager: CacheManager
  private lateinit var cache: Cache
  private lateinit var openaiApiService: OpenaiApiService
  private lateinit var internalProperties: InternalProperties
  private lateinit var currentDateProvider: CurrentDateProvider
  private lateinit var restTemplateBuilder: RestTemplateBuilder

  private val orgId = 1L

  @BeforeEach
  fun setUp() {
    llmPropertiesService = mock()
    llmProviderRepository = mock()
    cacheManager = mock()
    cache = mock()
    openaiApiService = mock()
    internalProperties = mock()
    currentDateProvider = mock()
    restTemplateBuilder = mock()

    whenever(cacheManager.getCache(Caches.LLM_PROVIDERS)).thenReturn(cache)
    whenever(cache.get(any<String>(), any<Class<*>>())).thenReturn(null)
    whenever(currentDateProvider.date).thenReturn(Date())
    whenever(internalProperties.fakeLlmProviders).thenReturn(true)
    whenever(openaiApiService.defaultAttempts()).thenReturn(listOf(30))
    whenever(llmProviderRepository.getAll(orgId)).thenReturn(emptyList())

    val restTemplate = mock<RestTemplate>()
    whenever(restTemplateBuilder.readTimeout(any())).thenReturn(restTemplateBuilder)
    whenever(restTemplateBuilder.build()).thenReturn(restTemplate)

    val resolver =
      LlmProviderResolver(
        llmProviderRepository = llmProviderRepository,
        llmPropertiesService = llmPropertiesService,
      )

    service =
      LlmProviderService(
        llmProviderRepository = llmProviderRepository,
        organizationService = mock(),
        llmPropertiesService = llmPropertiesService,
        openaiApiService = openaiApiService,
        tolgeeApiService = mock<TolgeeApiService>(),
        cacheManager = cacheManager,
        currentDateProvider = currentDateProvider,
        restTemplateBuilder = restTemplateBuilder,
        internalProperties = internalProperties,
        anthropicApiService = mock<AnthropicApiService>(),
        googleAiApiService = mock<GoogleAiApiService>(),
        llmProviderResolver = resolver,
      )
  }

  @Test
  fun `provider found on first try - no fallback used`() {
    setupServerProviders("openai")

    val result = service.callProvider(orgId, "openai", createParams())

    assertThat(result.response).contains("openai")
  }

  @Test
  fun `fallback resolves when primary provider not found`() {
    setupServerProviders("anthropic")
    setupFallbacks("openai" to "anthropic")

    val result = service.callProvider(orgId, "openai", createParams())

    assertThat(result.response).contains("anthropic")
  }

  @Test
  fun `chain of length 2 resolves`() {
    setupServerProviders("google-ai")
    setupFallbacks("openai" to "anthropic", "anthropic" to "google-ai")

    val result = service.callProvider(orgId, "openai", createParams())

    assertThat(result.response).contains("google-ai")
  }

  @Test
  fun `throws when no fallback configured`() {
    setupServerProviders()

    assertThatThrownBy {
      service.callProvider(orgId, "openai", createParams())
    }.isInstanceOf(LlmProviderNotFoundException::class.java)
  }

  @Test
  fun `circular fallback chain throws - no infinite loop`() {
    setupServerProviders()
    setupFallbacks("openai" to "anthropic", "anthropic" to "openai")

    assertThatThrownBy {
      service.callProvider(orgId, "openai", createParams())
    }.isInstanceOf(LlmProviderNotFoundException::class.java)
  }

  private fun setupServerProviders(vararg names: String) {
    whenever(llmPropertiesService.getProviders()).thenReturn(
      names.mapIndexed { index, name ->
        io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider(
          name = name,
          type = LlmProviderType.OPENAI,
        )
      },
    )
  }

  private fun setupFallbacks(vararg pairs: Pair<String, String>) {
    for ((from, to) in pairs) {
      whenever(llmPropertiesService.getFallbackProviderName(from)).thenReturn(to)
    }
  }

  private fun createParams(): LlmParams {
    return LlmParams(
      messages =
        listOf(
          LlmParams.Companion.LlmMessage(
            type = LlmParams.Companion.LlmMessageType.TEXT,
            text = "Translate 'hello' to Czech",
          ),
        ),
      shouldOutputJson = false,
      priority = LlmProviderPriority.HIGH,
    )
  }
}
