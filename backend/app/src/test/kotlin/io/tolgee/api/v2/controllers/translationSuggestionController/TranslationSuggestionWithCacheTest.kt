package io.tolgee.api.v2.controllers.translationSuggestionController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.providers.LlmTranslationProvider
import io.tolgee.component.machineTranslation.providers.ProviderTranslateParams
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import java.util.Date

@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN",
  ],
)
class TranslationSuggestionWithCacheTest : ProjectAuthControllerTest("/v2/projects/") {
  private var previousFreeCreditsAmount: Long? = null
  lateinit var testData: SuggestionTestData

  @Autowired
  @MockitoBean
  lateinit var llmTranslationProvider: LlmTranslationProvider

  @Autowired
  @MockitoBean
  lateinit var eeSubscriptionInfoProvider: EeSubscriptionInfoProvider

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  override lateinit var cacheManager: CacheManager

  lateinit var tolgeeTranslateParamsCaptor: KArgumentCaptor<ProviderTranslateParams>

  @BeforeEach
  fun setup() {
    previousFreeCreditsAmount = machineTranslationProperties.freeCreditsAmount
    Mockito.clearInvocations(llmTranslationProvider)
    setForcedDate(Date())
    initTestData()
    initMachineTranslationProperties(1000)
    initMachineTranslationMocks()
    doAnswer { true }.whenever(eeSubscriptionInfoProvider).isSubscribed()
    mockDefaultMtBucketSize(1000)
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
    previousFreeCreditsAmount?.let { machineTranslationProperties.freeCreditsAmount = it }
  }

  private fun mockDefaultMtBucketSize(size: Long) {
    machineTranslationProperties.freeCreditsAmount = size
  }

  private fun initMachineTranslationMocks() {
    tolgeeTranslateParamsCaptor = argumentCaptor()

    whenever(
      llmTranslationProvider.translate(
        tolgeeTranslateParamsCaptor.capture(),
      ),
    ).thenAnswer {
      MtValueProvider.MtResult(
        "Translated with LLM Prompt",
        ((it.arguments[0] as? ProviderTranslateParams)?.text?.length ?: 0) * 100,
      )
    }
  }

  private fun initTestData() {
    testData = SuggestionTestData()
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `translating is optimized`() {
    mockDefaultMtBucketSize(6000)
    testData.enablePrompt()
    testData.addAiDescriptions()
    saveTestData()

    performMtRequest().andIsOk

    performMtRequest().andIsOk

    println()
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  private fun performMtRequest(services: List<MtServiceType>? = null): ResultActions {
    return performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(
        keyId = testData.beautifulKey.id,
        targetLanguageId = testData.germanLanguage.id,
        services = services?.toSet(),
      ),
    )
  }
}
