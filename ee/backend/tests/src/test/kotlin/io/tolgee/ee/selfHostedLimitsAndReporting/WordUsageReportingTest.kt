package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.WordCountLimitTestData
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageToReportService
import io.tolgee.ee.stubs.TolgeeCloudLicencingClientStub
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.Date

@SpringBootTest()
class WordUsageReportingTest : AbstractSpringTest() {
  @Autowired
  private lateinit var usageToReportService: UsageToReportService

  @Autowired
  private lateinit var usageReportingService: UsageReportingService

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var tolgeeCloudLicencingClientStub: TolgeeCloudLicencingClientStub

  @Autowired
  private lateinit var organizationStatsService: OrganizationStatsService

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

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

  @Test
  fun `it reports words usage on translation edit`() {
    testWithTestData { testData, captor ->
      val translation =
        testData.projectBuilder.data.translations
          .first()
          .self

      translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(5))
      captor.assertWords(organizationStatsService.countAllWordsOnInstance())
    }
  }

  @Test
  fun `it reports word usage when project is deleted`() {
    testWithTestData { testData, captor ->
      projectService.deleteProject(testData.projectBuilder.self.id)

      // The key and word listeners react to the same deletion event, so only one of them
      // wins the immediate deferred-reporting window. Flush the other's stored value via
      // the periodic catch-up report to get a deterministic assertion.
      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)
    }
  }

  @Test
  fun `it reports word usage when organization is deleted`() {
    testWithTestData { testData, captor ->
      organizationService.delete(testData.projectBuilder.self.organizationOwner)

      currentDateProvider.move(Duration.ofDays(1))
      usageReportingService.reportIfNeeded()
      captor.assertWords(0)
    }
  }

  @Test
  fun `it does not report when billing is enabled (cloud)`() {
    whenever(billingConfProvider()).thenReturn(PublicBillingConfigurationDTO(enabled = true))

    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)
    currentDateProvider.move(Duration.ofDays(1))

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        val translation =
          testData.projectBuilder.data.translations
            .first()
            .self
        translationService.setTranslationText(translation, WordCountLimitTestData.wordsText(5))
        captor.allValues.assert.isEmpty()
      }
    }

    testDataService.cleanTestData(testData.root)
  }

  private fun testWithTestData(test: (WordCountLimitTestData, KArgumentCaptor<HttpEntity<*>>) -> Unit) {
    saveSubscription()
    val testData = WordCountLimitTestData(initialWordCount = 2)
    testDataService.saveTestData(testData.root)

    // Creating the key/translation via the test data builder already fires the deferred
    // reporting listeners once, so move time forward past the 1-minute deferral window.
    currentDateProvider.move(Duration.ofDays(1))

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        test(testData, captor)
      }
    }
    testDataService.cleanTestData(testData.root)
  }

  private fun saveSubscription() {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ACTIVE
        currentPeriodEnd = Date()
        enabledFeatures = Feature.entries.toTypedArray()
        lastValidCheck = Date()
        isPayAsYouGo = true
        includedKeys = 10
        includedSeats = 10
        keysLimit = 10
        seatsLimit = 10
        includedWords = 100
        wordsLimit = -1
      },
    )
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertWords(words: Long) {
    val data = parseRequestArgs()
    data["words"].toString().assert.isEqualTo(words.toString())
  }

  private fun KArgumentCaptor<HttpEntity<*>>.parseRequestArgs(): Map<*, *> =
    objectMapper.readValue(this.lastValue.body as String, Map::class.java)
}
