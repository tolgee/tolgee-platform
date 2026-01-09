package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.stubs.TolgeeCloudLicencingClientStub
import io.tolgee.model.UserAccount
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
class SeatUsageReportingTest : AbstractSpringTest() {
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
    eeLicenseMockRequestUtil = EeLicensingMockRequestUtil(restTemplate)
    tolgeeCloudLicencingClientStub.enableReporting = false
  }

  @Test
  fun `it reports seat usage`() {
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
        val user1 = createUser(1)
        captor.assertSeats(1)

        // we need to move time, because the reporting is deferred
        currentDateProvider.move(Duration.ofDays(1))
        val user2 = createUser(2)
        captor.assertSeats(2)

        currentDateProvider.move(Duration.ofDays(1))
        userAccountService.delete(user1.id)
        captor.assertSeats(1)

        currentDateProvider.move(Duration.ofDays(1))
        userAccountService.disable(user2.id)
        captor.assertSeats(0)
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

  private fun saveSubscription() {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ERROR
        currentPeriodEnd = Date()
        enabledFeatures = Feature.values()
        lastValidCheck = Date()
        isPayAsYouGo = true
        includedKeys = 10
        includedSeats = 10
        keysLimit = 10
        seatsLimit = 10
      },
    )
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertSeats(seats: Long) {
    val data = parseRequestArgs()
    data["seats"].toString().assert.isEqualTo(seats.toString())
  }

  private fun KArgumentCaptor<HttpEntity<*>>.parseRequestArgs(): Map<*, *> =
    objectMapper.readValue(this.lastValue.body as String, Map::class.java)
}
