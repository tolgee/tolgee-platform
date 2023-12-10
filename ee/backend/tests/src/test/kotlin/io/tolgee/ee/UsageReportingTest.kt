package io.tolgee.ee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.ee.data.SubscriptionStatus
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import java.util.*

class UsageReportingTest : AbstractSpringTest() {

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

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
      }
    )

    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        val user1 = userAccountService.createUser(
          SignUpDto(
            "Test",
            email = "aa@a.a",
            organizationName = "Ho",
            password = "12345678"
          )
        )
        captor.assertSeats(1)
        val user2 = userAccountService.createUser(
          SignUpDto(
            "Test",
            email = "ab@a.a",
            organizationName = "Ho",
            password = "12345678"
          )
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
