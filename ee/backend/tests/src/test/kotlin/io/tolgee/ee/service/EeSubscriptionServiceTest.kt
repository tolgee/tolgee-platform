package io.tolgee.ee.service

import io.tolgee.AbstractSpringTest
import io.tolgee.constants.Feature
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.data.SubscriptionStatus
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import java.util.*

@Suppress("SpringBootApplicationProperties")
@SpringBootTest(properties = ["tolgee.ee.check-period-ms=10"])
class EeSubscriptionServiceTest : AbstractSpringTest() {

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var eeLicensingMockRequestUtil: EeLicensingMockRequestUtil

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
        this.url = { it.contains("/v2/public/licensing/subscription") }
      }

      thenAnswer {
        eeLicenseMockRequestUtil.mockedSubscriptionResponse
      }

      verify {
        Thread.sleep(1020) // initialDelay + checkPeriod
        captor.allValues.assert.hasSizeGreaterThan(0)
        eeSubscriptionRepository.findAll().single().status.assert.isEqualTo(SubscriptionStatus.ACTIVE)
      }
    }
  }
}
