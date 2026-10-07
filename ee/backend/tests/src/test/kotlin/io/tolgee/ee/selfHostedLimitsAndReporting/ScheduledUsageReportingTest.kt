package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.component.SchedulingManager
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.usageReporting.ScheduledReportingManager
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageToReportService
import io.tolgee.ee.stubs.TolgeeCloudLicencingClientStub
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.UserAccount
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.Date
import kotlin.reflect.jvm.javaMethod

@SpringBootTest
@TestMethodOrder(OrderAnnotation::class)
@TestPropertySource(
  properties = [
    "tolgee.ee.reportUsageFixedDelayInMs=100",
    "tolgee.ee.scheduled-reporting-enabled=true",
  ],
)
class ScheduledUsageReportingTest : AbstractSpringTest() {
  companion object {
    @BeforeAll
    @JvmStatic
    fun before() {
      // We need to clear all scheduled tasks, because we can have some from other Spring Contexts created with
      // other test classes
      SchedulingManager.Companion.cancelAll()
    }
  }

  @Autowired
  private lateinit var usageToReportService: UsageToReportService

  @Autowired
  private lateinit var eeSubscriptionServiceImpl: EeSubscriptionServiceImpl

  @Autowired
  private lateinit var tolgeeCloudLicencingClientStub: TolgeeCloudLicencingClientStub

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @Autowired
  @MockitoSpyBean
  private lateinit var scheduledReportingManager: ScheduledReportingManager

  @BeforeEach
  fun setup() {
    eeLicenseMockRequestUtil = EeLicensingMockRequestUtil(restTemplate)
  }

  @AfterEach
  fun cleanup() {
    Mockito.reset(scheduledReportingManager)
    tolgeeCloudLicencingClientStub.enableReporting = false
  }

  /**
   * We canceled all scheduled tasks in @BeforeAll, to make sure that it's really scheduled, we need to test
   * whether the scheduleReporting method is called on startup
   */
  @Test
  @Order(1)
  fun `it schedules on startup`() {
    val invocations =
      Mockito
        .mockingDetails(scheduledReportingManager)
        .invocations
    invocations
      .filter { it.method == ScheduledReportingManager::scheduleReporting.javaMethod }
      .assert
      .hasSize(1)
  }

  @Test
  @Order(2)
  fun `it reports usage periodically`() {
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)
    saveSubscription()

    tolgeeCloudLicencingClientStub.enableReporting = true
    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        val baseSize = captor.allValues.size
        usageToReportService.delete()
        var reportedBefore = storedReportedAt()
        // scheduled only now, once the request mock is in place: a tick before it would report
        // outside the captor and defer the next report by a minute
        scheduledReportingManager.scheduleReporting()
        waitForReportStored(reportedBefore) { captor.allValues.assert.hasSize(baseSize + 1) }

        keyService.create(testData.project, "key1", null)

        // It doesn't report until we move time
        Thread.sleep(200)
        captor.allValues.assert.hasSize(baseSize + 1)

        reportedBefore = storedReportedAt()
        currentDateProvider.move(Duration.ofDays(1))
        waitForReportStored(reportedBefore) { captor.allValues.assert.hasSize(baseSize + 2) }

        createUser(1)

        // It doesn't report until we move time
        Thread.sleep(200)
        captor.allValues.assert.hasSize(baseSize + 2)

        reportedBefore = storedReportedAt()
        currentDateProvider.move(Duration.ofDays(1))
        waitForReportStored(reportedBefore) { captor.allValues.assert.hasSize(baseSize + 3) }
      }
    }
  }

  // The captor sees the POST before the scheduler stores reportedAt; a key or user created in
  // that gap reads the stale timestamp and reports at once instead of deferring. The baseline
  // is taken before the step that triggers the report, since a tick can store it within 100 ms.
  private fun waitForReportStored(
    reportedBefore: Date,
    reportSent: () -> Unit,
  ) {
    waitForNotThrowing(timeout = 30_000, pollTime = 100) {
      reportSent()
      storedReportedAt().assert.isAfter(reportedBefore)
    }
  }

  // straight from the table: the service's read is cached and would serve a stale timestamp
  private fun storedReportedAt(): Date =
    entityManager
      .createQuery("select lru.reportedAt from UsageToReport lru", Date::class.java)
      .resultList
      .singleOrNull() ?: Date(0)

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
