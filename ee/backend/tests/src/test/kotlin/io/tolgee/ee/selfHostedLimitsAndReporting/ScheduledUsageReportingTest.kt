package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageToReportService
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.UserAccount
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpMethod
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.Date

@TestPropertySource(properties = ["tolgee.ee.reportUsageFixedDelayInMs=100"])
class ScheduledUsageReportingTest : AbstractSpringTest() {
  @Autowired
  private lateinit var usageToReportService: UsageToReportService

  @Autowired
  private lateinit var eeSubscriptionServiceImpl: EeSubscriptionServiceImpl

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @BeforeEach
  fun setup() {
    eeLicenseMockRequestUtil = EeLicensingMockRequestUtil(restTemplate)
  }

  @Test
  fun `it reports usage periodically`() {
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    saveSubscription()
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        usageToReportService.delete()
        waitForNotThrowing(timeout = 10_000, pollTime = 100) {
          captor.allValues.assert.hasSize(1)
        }

        keyService.create(testData.project, "key1", null)

        // It doesn't report until we move time
        Thread.sleep(200)
        captor.allValues.assert.hasSize(1)

        currentDateProvider.move(Duration.ofDays(1))
        waitForNotThrowing(timeout = 10_000, pollTime = 100) {
          captor.allValues.assert.hasSize(2)
        }

        createUser(1)

        // It doesn't report until we move time
        Thread.sleep(200)
        captor.allValues.assert.hasSize(2)

        currentDateProvider.move(Duration.ofDays(1))
        waitForNotThrowing(timeout = 10_000, pollTime = 100) {
          captor.allValues.assert.hasSize(3)
        }
      }
    }
  }

  private fun createUser(idx: Long): UserAccount =
    userAccountService.createUserWithPassword(
      UserAccount(
        name = "Test",
        username = "$idx@a.a",
      ),
      rawPassword = "12345678",
    )

  private fun saveSubscription(): EeSubscription {
    return eeSubscriptionServiceImpl.save(
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
}
