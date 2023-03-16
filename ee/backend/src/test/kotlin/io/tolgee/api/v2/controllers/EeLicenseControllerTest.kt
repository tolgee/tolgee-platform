package io.tolgee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.constants.Feature
import io.tolgee.ee.api.v2.hateoas.SelfHostedEePlanModel
import io.tolgee.ee.api.v2.hateoas.SelfHostedEeSubscriptionModel
import io.tolgee.ee.data.SubscriptionStatus
import io.tolgee.ee.service.EeSubscriptionService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class EeLicenseControllerTest : AuthorizedControllerTest() {

  @BeforeEach
  fun setup() {
    var user: UserAccount? = null
    testDataService.saveTestData {
      addUserAccount {
        username = "franta"
        role = UserAccount.Role.ADMIN
        user = this
      }
    }
    userAccount = user
  }

  @MockBean
  @Autowired
  private lateinit var restTemplate: RestTemplate

  @Autowired
  private lateinit var eeSubscriptionService: EeSubscriptionService

  @Test
  fun `it set's license key`() {
    val mockedResponse = SelfHostedEeSubscriptionModel(
      id = 19919,
      currentPeriodEnd = 1624313600000,
      createdAt = 1624313600000,
      plan = SelfHostedEePlanModel(
        id = 19919,
        name = "Tolgee",
        public = true,
        enabledFeatures = arrayOf(Feature.PREMIUM_SUPPORT),
        pricePerSeat = 20.toBigDecimal(),
        subscriptionPrice = 200.toBigDecimal(),
      ),
      status = SubscriptionStatus.ACTIVE,
      licenseKey = "mocked_license_key",
      estimatedCosts = 200.toBigDecimal(),
    )

    val captor = mockPostRequest(
      "/v2/public/licensing/set-key", mockedResponse
    )
    performAuthPut("/v2/ee-license/set-license-key", mapOf("licenseKey" to "mock-mock"))
      .andIsOk.andPrettyPrint.andAssertThatJson {
      }
    val body = captor.allValues.single().body as String
    val req: Map<String, Any> =
      jacksonObjectMapper().readValue(body, Map::class.java) as Map<String, Any>

    req["licenseKey"].assert.isEqualTo("mock-mock")
    req["seats"].assert.isEqualTo(1)

    eeSubscriptionService.getSubscription().assert.isNotNull
  }

  @Test
  fun `it set's license key is not sensitive for non-breaking API change`() {
    performAuthPost("/v2/ee-license/set-license-key", mapOf("" to "")).andIsOk
  }

  private inline fun <reified Res> mockPostRequest(
    url: String,
    response: Res
  ): KArgumentCaptor<HttpEntity<*>> {
    val response = ResponseEntity(jacksonObjectMapper().writeValueAsString(response), HttpStatus.OK)
    val captor = argumentCaptor<HttpEntity<*>>()
    whenever(
      restTemplate.exchange(
        argThat<String> { this.contains(url) },
        eq(HttpMethod.POST),
        captor.capture(),
        eq(String::class.java)
      )
    ).thenReturn(response)
    return captor
  }
}
