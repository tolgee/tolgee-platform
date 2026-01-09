package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageToReportService
import io.tolgee.ee.stubs.TolgeeCloudLicencingClientStub
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.Date

@SpringBootTest()
class KeyUsageReportingTest : AbstractSpringTest() {
  @Autowired
  private lateinit var usageToReportService: UsageToReportService

  @Autowired
  private lateinit var usageReportingService: UsageReportingService

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var tolgeeCloudLicencingClientStub: TolgeeCloudLicencingClientStub

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @BeforeEach
  fun setup() {
    tolgeeCloudLicencingClientStub.enableReporting = false
    eeLicenseMockRequestUtil = EeLicensingMockRequestUtil(restTemplate)
    usageToReportService.delete()
  }

  @Test
  fun `it reports keys usage`() {
    testWithBaseTestData { testData, captor ->
      // key add & delete
      val key = keyService.create(testData.project, "key1", null)
      captor.assertKeys(1)

      // we need to move time, because the reporting is deferred
      currentDateProvider.move(Duration.ofDays(1))
      keyService.delete(key.id)
      captor.assertKeys(0)
    }
  }

  @Test
  fun `it doesn't report that often`() {
    testWithBaseTestData { testData, captor ->
      // key add and delete
      keyService.create(testData.project, "key1", null)
      captor.assertKeys(1)
      keyService.create(testData.project, "key2", null)
      captor.assertKeys(1)

      currentDateProvider.move(Duration.ofDays(1))
      // normally, the reportPeriodically is called automatically with scheduling
      usageReportingService.reportIfNeeded()
      captor.assertKeys(2)
    }
  }

  @Test
  fun `it does not execute many requests`() {
    testWithBaseTestData { testData, captor ->
      // create 10 keys
      executeInNewTransaction {
        (1..10).forEach {
          keyService.create(testData.project, "key$it", null)
        }
      }

      // now we have reported 10 keys
      captor.assertKeys(10)

      // check it doesn't do request for every key
      captor.allValues.assert.hasSizeLessThan(10)
    }
  }

  @Test
  fun `it reports key usage when project is deleted`() {
    testWithBaseTestData { testData, captor ->
      keyService.create(testData.project, "key1", null)

      // we need to move time, because the reporting is deferred
      currentDateProvider.move(Duration.ofDays(1))

      // delete the project
      projectService.deleteProject(testData.project.id)
      captor.assertKeys(0)
    }
  }

  @Test
  fun `it reports usage when organization is deleted`() {
    testWithBaseTestData { testData, captor ->
      keyService.create(testData.project, "key1", null)
      // we need to move time, because the reporting is deferred
      currentDateProvider.move(Duration.ofDays(1))
      // delete the organization
      organizationService.delete(testData.projectBuilder.self.organizationOwner)
      captor.assertKeys(0)
    }
  }

  private fun testWithBaseTestData(test: (BaseTestData, KArgumentCaptor<HttpEntity<*>>) -> Unit) {
    saveSubscription()
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)

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
      },
    )
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertKeys(keys: Long) {
    val data = parseRequestArgs()
    data["keys"].toString().assert.isEqualTo(keys.toString())
  }

  private fun KArgumentCaptor<HttpEntity<*>>.parseRequestArgs(): Map<*, *> =
    objectMapper.readValue(this.lastValue.body as String, Map::class.java)
}
