package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.fixtures.HttpClientMocker
import io.tolgee.fixtures.NdJsonParser
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.ignoreTestOnSpringBug
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.addDays
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@SpringBootTest()
@MockitoSpyBean(types = [InternalProperties::class])
class CreditLimitTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  private lateinit var testData: TestData

  private lateinit var httpClientMocker: HttpClientMocker

  @Autowired
  @MockitoSpyBean
  override lateinit var internalProperties: InternalProperties

  @MockitoBean
  @Autowired
  lateinit var restTemplateBuilder: RestTemplateBuilder

  @Autowired
  @MockitoBean
  private lateinit var restTemplate: RestTemplate

  @BeforeEach
  fun setup() {
    saveTestDataAndPrepare()
    httpClientMocker = HttpClientMocker(restTemplate)
    initMachineTranslationProperties(
      freeCreditsAmount = -1,
    )
    whenever(internalProperties.fakeMtProviders).thenReturn(false)
    whenever(restTemplateBuilder.readTimeout(any())).thenReturn(restTemplateBuilder)
    whenever(restTemplateBuilder.build()).thenReturn(restTemplate)
  }

  @ProjectJWTAuthTestMethod
  fun `correctly propagates credit spending limit exceeded`() {
    testPropagatesError(Message.CREDIT_SPENDING_LIMIT_EXCEEDED.code)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `correctly propagates out of credits errors`() {
    testPropagatesError(Message.OUT_OF_CREDITS.code)
  }

  fun testPropagatesError(errorCode: String) {
    saveSubscription()
    whenever(
      restTemplate.exchange(
        anyString(),
        eq(POST),
        any(),
        eq(PromptResult::class.java),
      ),
    ).thenThrow(
      mockBadRequest(errorCode),
    )

    val response =
      ignoreTestOnSpringBug {
        performProjectAuthPost(
          "suggest/machine-translations-streaming",
          mapOf(
            "targetLanguageId" to testData.czechLanguage.id,
            "baseText" to "text",
          ),
        ).andDo {
          it.getAsyncResult(10000)
        }.andIsOk
          .andReturn()
          .response.contentAsString
      }
    val parsed = NdJsonParser(objectMapper).parse(response)
    parsed.assert.hasSize(3)
    (parsed[1] as Map<*, *>)["errorMessage"].assert.isEqualTo(errorCode)
  }

  private fun mockBadRequest(errorCode: String): HttpClientErrorException {
    val badRequestMock =
      HttpClientErrorException.create(
        HttpStatus.BAD_REQUEST,
        "Mocked response",
        HttpHeaders(),
        """{"code": "$errorCode"}""".toByteArray(),
        null,
      )
    return badRequestMock
  }

  private fun saveSubscription(build: EeSubscription.() -> Unit = {}) {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ACTIVE
        currentPeriodEnd = currentDateProvider.date.addDays(-1)
        enabledFeatures = Feature.entries.toTypedArray()
        lastValidCheck = currentDateProvider.date
        isPayAsYouGo = false
        build(this)
      },
    )
  }

  private fun saveTestDataAndPrepare() {
    testData = TestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.project }
  }

  private class TestData : BaseTestData() {
    val czechLanguage = projectBuilder.addCzech().self
  }
}
