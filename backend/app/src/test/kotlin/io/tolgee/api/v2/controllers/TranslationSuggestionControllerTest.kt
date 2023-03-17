package io.tolgee.api.v2.controllers

import com.amazonaws.services.translate.AmazonTranslate
import com.amazonaws.services.translate.model.TranslateTextResult
import com.google.cloud.translate.Translate
import com.google.cloud.translate.Translation
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.machineTranslation.providers.AzureCognitiveApiService
import io.tolgee.component.machineTranslation.providers.BaiduApiService
import io.tolgee.component.machineTranslation.providers.DeeplApiService
import io.tolgee.component.mtBucketSizeProvider.MtBucketSizeProvider
import io.tolgee.constants.Caches
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.mapResponseTo
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.apache.commons.lang3.time.DateUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.test.web.servlet.ResultActions
import java.util.*
import kotlin.system.measureTimeMillis

class TranslationSuggestionControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionTestData

  @Autowired
  @MockBean
  lateinit var currentDateProvider: CurrentDateProvider

  @Autowired
  @MockBean
  lateinit var mtBucketSizeProvider: MtBucketSizeProvider

  @Autowired
  @MockBean
  lateinit var googleTranslate: Translate

  @Autowired
  @MockBean
  lateinit var amazonTranslate: AmazonTranslate

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
  lateinit var cacheManager: CacheManager

  lateinit var cacheMock: Cache

  @BeforeEach
  fun setup() {
    mockCurrentDate { Date() }
    initTestData()
    initMachineTranslationProperties(1000)
    initMachineTranslationMocks()
    mockDefaultMtBucketSize(1000)
    cacheMock = mock()
    val rateLimitsCacheMock = mock<Cache>()
    whenever(cacheManager.getCache(eq(Caches.RATE_LIMITS))).thenReturn(rateLimitsCacheMock)
    whenever(cacheManager.getCache(eq(Caches.MACHINE_TRANSLATIONS))).thenReturn(cacheMock)
  }

  private fun mockDefaultMtBucketSize(size: Long) {
    whenever(mtBucketSizeProvider.getSize(anyOrNull())).thenAnswer {
      size
    }
  }

  private fun initMachineTranslationMocks() {
    val googleTranslationMock = mock() as Translation
    val awsTranslateTextResult = mock() as TranslateTextResult

    whenever(
      googleTranslate.translate(
        any<String>(),
        any(),
        any(),
        any()
      )
    ).thenReturn(googleTranslationMock)

    whenever(googleTranslationMock.translatedText).thenReturn("Translated with Google")

    whenever(amazonTranslate.translateText(any())).thenReturn(awsTranslateTextResult)

    whenever(awsTranslateTextResult.translatedText).thenReturn("Translated with Amazon")

    whenever(
      deeplApiService.translate(
        any(),
        any(),
        any(),
      )
    ).thenReturn("Translated with DeepL")

    whenever(
      azureCognitiveApiService.translate(
        any(),
        any(),
        any(),
      )
    ).thenReturn("Translated with Azure Cognitive")

    whenever(
      baiduApiService.translate(
        any(),
        any(),
        any(),
      )
    ).thenReturn("Translated with Baidu")
  }

  private fun initTestData() {
    testData = SuggestionTestData()
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM with keyId`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(keyId = testData.thisIsBeautifulKey.id, targetLanguageId = testData.germanLanguage.id)
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.translationMemoryItems") {
        node("[0]") {
          node("targetText").isEqualTo("Das ist schön")
          node("baseText").isEqualTo("This is beautiful")
          node("keyName").isEqualTo("key 2")
          node("similarity").isEqualTo("0.6296296")
        }
      }
      node("page.totalElements").isEqualTo(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM with baseText`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(baseText = "This is beautiful", targetLanguageId = testData.germanLanguage.id)
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.translationMemoryItems") {
        node("[0]") {
          node("targetText").isEqualTo("Das ist schön")
          node("baseText").isEqualTo("This is beautiful")
          node("keyName").isEqualTo("key 2")
          node("similarity").isEqualTo("1.0")
        }
      }
      node("page.totalElements").isEqualTo(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM fast enough`() {
    testData.generateLotOfData()
    saveTestData()
    val time = measureTimeMillis {
      performAuthPost(
        "/v2/projects/${project.id}/suggest/translation-memory",
        SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id)
      ).andIsOk
    }
    assertThat(time).isLessThan(1500)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests machine translations with keyId`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id)
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("GOOGLE").isEqualTo("Translated with Google")
      }
      node("translationCreditsBalanceBefore").isEqualTo(1000)
      node("translationCreditsBalanceAfter").isEqualTo(1000 - "Beautiful".length * 100)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests machine translations with baseText`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(baseText = "Yupee", targetLanguageId = testData.germanLanguage.id)
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
      """
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests using just enabled services (Google, AWS, DeepL, Azure, Baidu)`() {
    mockDefaultMtBucketSize(5000)
    testData.enableAll()
    saveTestData()

    performMtRequest().andIsOk.andPrettyPrint.andAssertThatJson {
      node("machineTranslations") {
        node("AWS").isEqualTo("Translated with Amazon")
        node("GOOGLE").isEqualTo("Translated with Google")
        node("DEEPL").isEqualTo("Translated with DeepL")
        node("AZURE").isEqualTo("Translated with Azure Cognitive")
        node("BAIDU").isEqualTo("Translated with Baidu")
      }
      node("translationCreditsBalanceAfter").isEqualTo(500)
    }
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
        """
      )
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `primary service is first (AWS)`() {
    machineTranslationProperties.freeCreditsAmount = -1
    testData.enableAll()
    saveTestData()

    (0..20).forEach {
      verifyServiceFirst("AWS")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `primary service is first (GOOGLE)`() {
    machineTranslationProperties.freeCreditsAmount = -1
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

    mockCurrentDate { DateUtils.addMonths(Date(), 1) }
    testMtCreditConsumption()

    mockCurrentDate { DateUtils.addMonths(Date(), 2) }
    testMtCreditConsumption()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it consumes extra credits`() {
    testData.addBucketWithExtraCredits()
    saveTestData()
    performMtRequestAndExpectAfterBalance(100, 1000)
    performMtRequestAndExpectAfterBalance(0, 200)
    performMtRequestAndExpectBadRequest().andAssertThatJson {
      node("params[0]").isEqualTo("0")
      node("params[1]").isEqualTo("200")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it doesn't consume when cached`() {
    saveTestData()
    val valueWrapperMock = mock<Cache.ValueWrapper>()
    whenever(cacheMock.get(any())).thenReturn(valueWrapperMock)
    whenever(valueWrapperMock.get()).thenReturn("Yeey! Cached!")
    performMtRequestAndExpectAfterBalance(1000)
  }

  private fun testMtCreditConsumption() {
    performMtRequestAndExpectAfterBalance(100)
    performMtRequestAndExpectBadRequest()
  }

  private fun mockCurrentDate(dateProvider: () -> Date) {
    whenever(currentDateProvider.date).thenAnswer { dateProvider() }
  }

  private fun performMtRequestAndExpectAfterBalance(creditBalance: Int, extraCreditBalance: Int = 0) {
    performMtRequest().andIsOk.andAssertThatJson {
      node("translationCreditsBalanceAfter").isEqualTo(creditBalance)
      node("translationExtraCreditsBalanceAfter").isEqualTo(extraCreditBalance)
    }
  }

  private fun performMtRequestAndExpectBadRequest(): ResultActions {
    return performMtRequest().andIsBadRequest
  }

  private fun performMtRequest(): ResultActions {
    return performAuthPost(
      "/v2/projects/${project.id}/suggest/machine-translations",
      SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id)
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
