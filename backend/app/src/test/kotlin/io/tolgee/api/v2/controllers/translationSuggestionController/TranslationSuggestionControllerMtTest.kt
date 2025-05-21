package io.tolgee.api.v2.controllers.translationSuggestionController

import com.google.cloud.translate.Translate
import com.google.cloud.translate.Translation
import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.EeSubscriptionInfoProvider
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.machineTranslation.providers.*
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.ee.component.LLMTranslationProviderEeImpl
import io.tolgee.fixtures.*
import io.tolgee.model.mtServiceConfig.Formality
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.addMonths
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.test.web.servlet.ResultActions
import software.amazon.awssdk.services.translate.TranslateClient
import software.amazon.awssdk.services.translate.model.TranslateTextRequest
import software.amazon.awssdk.services.translate.model.TranslateTextResponse
import java.util.*
import software.amazon.awssdk.services.translate.model.Formality as AwsFormality

class TranslationSuggestionControllerMtTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionTestData

  @Autowired
  @MockBean
  lateinit var googleTranslate: Translate

  @Autowired
  @MockBean
  lateinit var amazonTranslate: TranslateClient

  @Autowired
  @MockBean
  lateinit var deeplApiService: DeeplApiService

  @Autowired
  @MockBean
  lateinit var azureCognitiveApiService: AzureCognitiveApiService

  @Autowired
  @MockBean
  lateinit var baiduApiService: BaiduApiService

  @Autowired
  @MockBean
  lateinit var llmTranslationProvider: LLMTranslationProviderEeImpl

  @Autowired
  @MockBean
  lateinit var eeSubscriptionInfoProvider: EeSubscriptionInfoProvider

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @MockBean
  override lateinit var cacheManager: CacheManager

  @Suppress("LateinitVarOverridesLateinitVar")
  @MockBean
  @Autowired
  override lateinit var machineTranslationProperties: MachineTranslationProperties

  lateinit var cacheMock: Cache

  lateinit var tolgeeTranslateParamsCaptor: KArgumentCaptor<ProviderTranslateParams>

  @BeforeEach
  fun setup() {
    Mockito.clearInvocations(amazonTranslate, deeplApiService, llmTranslationProvider)
    setForcedDate(Date())
    initTestData()
    initMachineTranslationProperties(1000)
    initMachineTranslationMocks()
    doAnswer { true }.whenever(eeSubscriptionInfoProvider).isSubscribed()
    mockDefaultMtBucketSize(1000)
    cacheMock = mock()
    val rateLimitsCacheMock = mock<Cache>()
    whenever(cacheManager.getCache(eq(Caches.RATE_LIMITS))).thenReturn(rateLimitsCacheMock)
    whenever(cacheManager.getCache(eq(Caches.MACHINE_TRANSLATIONS))).thenReturn(cacheMock)
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  private fun mockDefaultMtBucketSize(size: Long) {
    whenever(machineTranslationProperties.freeCreditsAmount).thenAnswer {
      size
    }
  }

  private fun initMachineTranslationMocks() {
    val googleTranslationMock = mock() as Translation
    val awsTranslateTextResult =
      TranslateTextResponse
        .builder()
        .translatedText("Translated with Amazon")
        .build()

    whenever(
      googleTranslate.translate(
        any<String>(),
        any(),
        any(),
        any(),
      ),
    ).thenReturn(googleTranslationMock)

    whenever(googleTranslationMock.translatedText).thenReturn("Translated with Google")

    whenever(amazonTranslate.translateText(any<TranslateTextRequest>())).thenReturn(awsTranslateTextResult)

    whenever(
      deeplApiService.translate(
        any(),
        any(),
        any(),
        any(),
      ),
    ).thenReturn("Translated with DeepL")

    whenever(
      azureCognitiveApiService.translate(
        any(),
        any(),
        any(),
      ),
    ).thenReturn("Translated with Azure Cognitive")

    whenever(
      baiduApiService.translate(
        any(),
        any(),
        any(),
      ),
    ).thenReturn("Translated with Baidu")

    tolgeeTranslateParamsCaptor = argumentCaptor()

    whenever(llmTranslationProvider.isEnabled).thenReturn(true)
    whenever(llmTranslationProvider.isLanguageSupported(any())).thenReturn(true)
    whenever(
      llmTranslationProvider.translate(
        tolgeeTranslateParamsCaptor.capture(),
      ),
    ).thenAnswer {
      MtValueProvider.MtResult(
        "Translated with Tolgee Translator",
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
  fun `it suggests machine translations with keyId`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("GOOGLE").isEqualTo("Translated with Google")
      }
      mtCreditBucketService.getCreditBalances(testData.projectBuilder.self.organizationOwner.id).creditBalance
        .assert.isEqualTo((1000 - "Beautiful".length * 100).toLong())
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests machine translations with baseText`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(baseText = "Yupee", targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("GOOGLE").isEqualTo("Translated with Google")
      }
    }
    verify(googleTranslate).translate(eq("Yupee"), any(), any(), any())
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests using just enabled services (AWS)`() {
    testData.enableAWS()
    saveTestData()
    performMtRequest().andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations").isEqualTo(
        """
        {
          "AWS": "Translated with Amazon"
        }
      """,
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests using all enabled services (Google, AWS, DeepL, Azure, Baidu, PROMPT)`() {
    mockDefaultMtBucketSize(6000)
    testData.enableAll()
    saveTestData()

    performMtRequest().andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("AWS").isEqualTo("Translated with Amazon")
        node("GOOGLE").isEqualTo("Translated with Google")
        node("DEEPL").isEqualTo("Translated with DeepL")
        node("AZURE").isEqualTo("Translated with Azure Cognitive")
        node("BAIDU").isEqualTo("Translated with Baidu")
        node("TOLGEE").isEqualTo("Translated with Tolgee Translator")
      }

      mtCreditBucketService.getCreditBalances(
        testData.projectBuilder.self.organizationOwner.id,
      ).creditBalance.assert.isEqualTo(
        600,
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests only using explicitly provided services`() {
    mockDefaultMtBucketSize(6000)
    testData.enableAll()
    saveTestData()

    performMtRequest(listOf(MtServiceType.AWS, MtServiceType.PROMPT)).andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("AWS").isEqualTo("Translated with Amazon")
        node("GOOGLE").isAbsent()
        node("DEEPL").isAbsent()
        node("AZURE").isAbsent()
        node("BAIDU").isAbsent()
        node("PROMPT").isEqualTo("Translated with Tolgee Translator")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it throws if service is disabled`() {
    mockDefaultMtBucketSize(6000)
    testData.enableAWS()
    saveTestData()

    performMtRequest(listOf(MtServiceType.PROMPT)).andIsBadRequest.andHasErrorMessage(Message.MT_SERVICE_NOT_ENABLED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it respects default config`() {
    machineTranslationProperties.freeCreditsAmount = 2000
    testData.addDefaultConfig()
    saveTestData()

    performMtRequest().andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations").isEqualTo(
        """
        {
          "AWS": "Translated with Amazon"
        }
        """,
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `primary service is first (AWS)`() {
    mockDefaultMtBucketSize(-1)
    testData.enableAll()
    saveTestData()

    (0..20).forEach {
      verifyServiceFirst("AWS")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `primary service is first (GOOGLE)`() {
    mockDefaultMtBucketSize(-1)
    testData.enableAllGooglePrimary()
    saveTestData()

    (0..20).forEach {
      verifyServiceFirst("GOOGLE")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it consumes and refills bucket`() {
    saveTestData()

    testMtCreditConsumption()

    setForcedDate(Date().addMonths(1))
    testMtCreditConsumption()

    setForcedDate(Date().addMonths(2))
    testMtCreditConsumption()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it consumes last positive credits, next time throws exception`() {
    mockDefaultMtBucketSize(200)
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("GOOGLE").isEqualTo("Translated with Google")
      }
    }
    performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id),
    ).andIsBadRequest
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it doesn't consume when cached`() {
    saveTestData()
    val valueWrapperMock = mock<Cache.ValueWrapper>()
    whenever(cacheMock.get(any())).thenReturn(valueWrapperMock)
    whenever(valueWrapperMock.get()).thenReturn(
      TranslateResult(
        translatedText = "Yeey! Cached!",
        contextDescription = "context",
        actualPrice = 100,
        usedService = MtServiceType.GOOGLE,
        baseBlank = false,
      ),
    )
    performMtRequestAndExpectAfterBalance(10)
  }

//  @Test
//  @ProjectJWTAuthTestMethod
//  fun `it uses Tolgee correctly`() {
//    mockDefaultMtBucketSize(6000)
//    testData.enableTolgee()
//    testData.addAiDescriptions()
//    saveTestData()
//
//    performMtRequest().andIsOk.andPrettyPrint.andAssertThatJson {
//      node("machineTranslations") {
//        node("TOLGEE").isEqualTo("Translated with Tolgee Translator")
//      }
//      mtCreditBucketService.getCreditBalances(testData.projectBuilder.self.organizationOwner.id).creditBalance
//        .assert.isEqualTo(5100)
//    }
//
//    tolgeeTranslateParamsCaptor.allValues.assert.hasSize(1)
//    val metadata = tolgeeTranslateParamsCaptor.firstValue
//    metadata!!.proj
//    metadata.closeItems.assert.hasSize(4)
//    metadata.keyDescription.assert.isEqualTo(testData.beautifulKey.keyMeta!!.description)
//    metadata.projectDescription.assert.isEqualTo(testData.project.aiTranslatorPromptDescription)
//    metadata.languageDescription.assert.isEqualTo(testData.germanLanguage.aiTranslatorPromptDescription)
//  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it uses correct Tolgee formality`() {
    mockDefaultMtBucketSize(6000)
    testData.enablePrompt(Formality.FORMAL)
    saveTestData()
    performMtRequest()
    tolgeeTranslateParamsCaptor.firstValue.formality.assert.isEqualTo(Formality.FORMAL)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it uses correct DeepL formality`() {
    mockDefaultMtBucketSize(6000)
    testData.enableDeepL(Formality.FORMAL)
    saveTestData()
    performMtRequest()
    val formality = Mockito.mockingDetails(deeplApiService).invocations.first().arguments[3] as? Formality
    formality.assert.isEqualTo(Formality.FORMAL)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it uses correct AWS formality`() {
    mockDefaultMtBucketSize(6000)
    testData.enableAWS(Formality.FORMAL)
    saveTestData()
    performMtRequest()
    val request =
      Mockito.mockingDetails(amazonTranslate).invocations.first().arguments[0]
        as TranslateTextRequest
    request.settings().formality().assert.isEqualTo(AwsFormality.FORMAL)
  }

  private fun testMtCreditConsumption() {
    performMtRequestAndExpectAfterBalance(1)
    performMtRequestAndExpectAfterBalance(0)
    performMtRequestAndExpectBadRequest()
  }

  private fun performMtRequestAndExpectAfterBalance(creditBalance: Long) {
    performMtRequest().andIsOk
    mtCreditBucketService.getCreditBalances(testData.projectBuilder.self.organizationOwner.id).creditBalance
      .assert.isEqualTo(creditBalance * 100)
  }

  private fun performMtRequestAndExpectBadRequest(): ResultActions {
    return performMtRequest().andIsBadRequest
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

  private fun verifyServiceFirst(service: String) {
    val result = performMtRequest().andIsOk.andReturn().mapResponseTo<Map<String, Any>>()
    val services = (result["machineTranslations"] as Map<String, String>).keys.toList()
    assertThat(services[0]).isEqualTo(service)
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }
}
