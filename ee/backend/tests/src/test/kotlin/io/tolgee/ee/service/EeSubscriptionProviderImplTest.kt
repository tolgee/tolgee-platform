package io.tolgee.ee.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.EeProperties
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import kotlin.properties.Delegates

@Suppress("SpringBootApplicationProperties")
class EeSubscriptionProviderImplTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeProperties: EeProperties

  @Autowired
  private lateinit var eeSubscriptionServiceImpl: EeSubscriptionServiceImpl

  @Autowired
  @MockitoBean
  lateinit var restTemplate: RestTemplate

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  private lateinit var eeLicenseMockRequestUtil: EeLicensingMockRequestUtil

  var oldCheckPeriodProperty by Delegates.notNull<Long>()

  @BeforeEach
  fun setup() {
    schedulingManager.cancelAll()
    setCheckPeriodProperty()
    eeSubscriptionServiceImpl.scheduleSubscriptionChecking()
    currentDateProvider.forcedDate = currentDateProvider.date
  }

  @AfterEach
  fun cleanup() {
    eeProperties.checkPeriodInMs = oldCheckPeriodProperty
  }

  @Test
  fun `it checks for subscription changes`() {
    prepareSubscription()

    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/subscription") }
      }

      thenAnswer {
        eeLicenseMockRequestUtil.mockedSubscriptionResponse
      }

      verify {
        waitForNotThrowing(pollTime = 100, timeout = 2000) {
          captor.allValues.assert.hasSizeGreaterThan(0)
          executeInNewTransaction {
            eeSubscriptionRepository
              .findAll()
              .single()
              .status.assert
              .isEqualTo(SubscriptionStatus.ACTIVE)
          }
        }
      }
    }
  }

  @Test
  fun `cancels subscription when other instance uses the key`() {
    prepareSubscription()

    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/subscription") }
      }

      thenThrow(
        HttpClientErrorException.create(
          "Bad request",
          HttpStatus.BAD_REQUEST,
          "Bad request",
          HttpHeaders(),
          jacksonObjectMapper()
            .writeValueAsString(
              ErrorResponseBody(
                Message.LICENSE_KEY_USED_BY_ANOTHER_INSTANCE.code,
                null,
              ),
            ).toByteArray(),
          null,
        ),
      )

      verify {
        waitForNotThrowing(pollTime = 100, timeout = 2000) {
          captor.allValues.assert.hasSizeGreaterThan(0)
          eeSubscriptionRepository
            .findAll()
            .single()
            .status
            .assert
            .isEqualTo(SubscriptionStatus.KEY_USED_BY_ANOTHER_INSTANCE)
        }
      }
    }
  }

  private fun setCheckPeriodProperty() {
    oldCheckPeriodProperty = eeProperties.checkPeriodInMs
    eeProperties.checkPeriodInMs = 10
  }

  private fun EeSubscriptionProviderImplTest.prepareSubscription() {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ACTIVE
        currentPeriodEnd = currentDateProvider.date
        enabledFeatures = Feature.entries.toTypedArray()
        lastValidCheck = currentDateProvider.date
      },
    )
  }
}
