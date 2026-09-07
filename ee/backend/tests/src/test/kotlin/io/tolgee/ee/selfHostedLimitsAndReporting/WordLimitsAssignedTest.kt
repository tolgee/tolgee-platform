package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.development.testDataBuilder.data.WordCountLimitTestData
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageToReportService
import io.tolgee.ee.stubs.TolgeeCloudLicencingClientStub
import io.tolgee.hateoas.ee.SelfHostedEePlanModel
import io.tolgee.hateoas.ee.SelfHostedEeSubscriptionModel
import io.tolgee.hateoas.limits.LimitModel
import io.tolgee.hateoas.limits.SelfHostedUsageLimitsModel
import io.tolgee.publicBilling.MetricType
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Nothing counts words while a plan does not meter them, so a licence refresh that starts metering
 * has to ask for a fresh count — otherwise the cloud bills on whatever the keys-and-seats era left.
 */
@SpringBootTest
class WordLimitsAssignedTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var usageToReportService: UsageToReportService

  @Autowired
  private lateinit var eeSubscriptionService: EeSubscriptionServiceImpl

  @Autowired
  private lateinit var usageReportingService: UsageReportingService

  @Autowired
  private lateinit var tolgeeCloudLicencingClientStub: TolgeeCloudLicencingClientStub

  @MockitoBean
  @Autowired
  private lateinit var restTemplate: RestTemplate

  @MockitoBean
  @Autowired
  private lateinit var billingConfProvider: PublicBillingConfProvider

  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @BeforeEach
  fun setup() {
    tolgeeCloudLicencingClientStub.enableReporting = false
    whenever(billingConfProvider()).thenReturn(PublicBillingConfigurationDTO(enabled = false))
    eeLicenseMockRequestUtil = EeLicensingMockRequestUtil(restTemplate)
    usageToReportService.delete()
  }

  @AfterEach
  fun clearSubscription() {
    // The ee_subscription row is a singleton, so a subscription left behind here limits every test
    // class that runs after this one.
    eeSubscriptionRepository.deleteAll()
  }

  @Test
  fun `a refresh that starts metering words asks for a fresh count`() {
    val testData = givenLicensedInstanceNotMeteringWords()

    refreshWithWordLimits(LimitModel(100_000, 100_000))

    usageToReportService
      .findOrCreateUsageToReport()
      .wordsDirty.assert
      .isTrue()

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a refresh that leaves the word limits alone does not`() {
    val testData = givenLicensedInstanceNotMeteringWords()

    refreshWithWordLimits(LimitModel(-1, -1), metricType = MetricType.KEYS_SEATS)

    usageToReportService
      .findOrCreateUsageToReport()
      .wordsDirty.assert
      .isFalse()

    testDataService.cleanTestData(testData.root)
  }

  /**
   * The allowance carries no number, so nothing in [LimitModel] distinguishes this from the
   * keys-and-seats licence above — only the plan's metric does.
   */
  @Test
  fun `a refresh onto a word plan with a negotiated allowance still asks for a fresh count`() {
    val testData = givenLicensedInstanceNotMeteringWords()

    refreshWithWordLimits(LimitModel(-2, -2))

    usageToReportService
      .findOrCreateUsageToReport()
      .wordsDirty.assert
      .isTrue()

    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `activating a word licence tells the cloud the count and that it can report words`() {
    val testData = WordCountLimitTestData(initialWordCount = 4)
    testDataService.saveTestData(testData.root)
    usageToReportService.delete()

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/") }
      }

      thenAnswer { subscriptionResponse(LimitModel(100_000, 100_000)) }

      verify {
        eeSubscriptionService.setLicenceKey("mocked_license_key")

        val activation = objectMapper.readValue(captor.lastValue.body as String, Map::class.java)
        activation["words"].toString().assert.isEqualTo("4")
        activation["reportedMetrics"].toString().assert.contains("HOSTED_WORDS")

        // Whether the figure lands on the row or via the dirty flag, the first report carries it.
        currentDateProvider.move(Duration.ofDays(1))
        usageReportingService.reportIfNeeded()
        val report = objectMapper.readValue(captor.lastValue.body as String, Map::class.java)
        report["words"].toString().assert.isEqualTo("4")
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  private fun givenLicensedInstanceNotMeteringWords(): WordCountLimitTestData {
    eeSubscriptionRepository.save(
      SelfHostedSubscriptionFixture.activeSubscription {
        licenseKey = "mocked_license_key"
        includedKeys = -1
        keysLimit = -1
        includedSeats = -1
        seatsLimit = -1
        includedWords = -1
        wordsLimit = -1
      },
    )
    val testData = WordCountLimitTestData(initialWordCount = 4)
    testDataService.saveTestData(testData.root)
    executeInNewTransaction { usageToReportService.storeCurrentUsage(words = 0) }
    usageToReportService.takeWordsDirty()
    return testData
  }

  private fun subscriptionResponse(
    words: LimitModel,
    metricType: MetricType = MetricType.HOSTED_WORDS,
  ): SelfHostedEeSubscriptionModel =
    eeLicenseMockRequestUtil.mockedSubscriptionResponse.let {
      SelfHostedEeSubscriptionModel(
        id = it.id,
        currentPeriodEnd = it.currentPeriodEnd,
        createdAt = it.createdAt,
        plan = wordPlanModel(it.plan, metricType),
        status = it.status,
        licenseKey = it.licenseKey,
        estimatedCosts = it.estimatedCosts,
        currentPeriodStart = it.currentPeriodStart,
        limits =
          SelfHostedUsageLimitsModel(
            keys = LimitModel(-1, -1),
            seats = LimitModel(-1, -1),
            mtCreditsInCents = LimitModel(-1, -1),
            words = words,
          ),
      )
    }

  private fun wordPlanModel(
    plan: SelfHostedEePlanModel,
    metricType: MetricType,
  ) = SelfHostedEePlanModel(
    id = plan.id,
    name = plan.name,
    enabledFeatures = plan.enabledFeatures,
    prices = plan.prices,
    includedUsage = plan.includedUsage,
    free = plan.free,
    nonCommercial = plan.nonCommercial,
    isPayAsYouGo = plan.isPayAsYouGo,
    metricType = metricType,
  )

  private fun refreshWithWordLimits(
    words: LimitModel,
    metricType: MetricType = MetricType.HOSTED_WORDS,
  ) {
    val response = subscriptionResponse(words, metricType)

    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/subscription") }
      }
      thenAnswer { response }
      verify {
        eeSubscriptionService.refreshSubscription()
      }
    }
  }
}
