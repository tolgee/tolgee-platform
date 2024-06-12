package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.model.UserAccount
import io.tolgee.service.security.SignUpService
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.util.*

@SpringBootTest()
class UsageReportingTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @MockBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @Autowired
  private lateinit var eeLicensingMockRequestUtil: EeLicensingMockRequestUtil

  @Autowired
  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  @Autowired
  private lateinit var signUpService: SignUpService

  @Test
  fun `it checks for subscription changes`() {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ERROR
        currentPeriodEnd = Date()
        cancelAtPeriodEnd = false
        enabledFeatures = Feature.values()
        lastValidCheck = Date()
      },
    )

    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        val user1 =
          userAccountService.createUserWithPassword(
            UserAccount(
              name = "Test",
              username = "aa@a.a",
            ),
            rawPassword = "12345678",
          )
        captor.assertSeats(1)
        val user2 =
          userAccountService.createUserWithPassword(
            UserAccount(
              name = "Test",
              username = "ab@a.a",
            ),
            rawPassword = "12345678",
          )
        captor.assertSeats(2)
        userAccountService.delete(user1.id)
        captor.assertSeats(1)
        userAccountService.disable(user2.id)
        captor.assertSeats(0)
      }
    }
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertSeats(seats: Long) {
    val data = jacksonObjectMapper().readValue(this.lastValue.body as String, Map::class.java)
    data["seats"].toString().assert.isEqualTo(seats.toString())
  }
}
