package io.tolgee.ee.api.v2.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.util.Date

class EeLicenseControllerTest : AuthorizedControllerTest() {
  @Autowired
  @MockitoBean
  lateinit var restTemplate: RestTemplate

  @BeforeEach
  fun setup() {
    var user: UserAccount? = null
    testDataService.saveTestData {
      addUserAccount {
        username = "franta"
        role = UserAccount.Role.ADMIN
        user = this
      }
      addProject { name = "test" }.build {
        addKey("hehe")
      }
    }
    userAccount = user
  }

  @Autowired
  lateinit var eeLicensingMockRequestUtil: EeLicensingMockRequestUtil

  @Autowired
  private lateinit var eeSubscriptionService: EeSubscriptionServiceImpl

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Test
  fun `it set's license key`() {
    eeLicensingMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/set-key") }
      }

      thenAnswer {
        eeLicensingMockRequestUtil.mockedSubscriptionResponse
      }

      verify {
        performAuthPut("/v2/ee-license/set-license-key", mapOf("licenseKey" to "mock-mock"))
          .andIsOk.andPrettyPrint
          .andAssertThatJson {
          }
        val body = captor.allValues.single().body as String

        @Suppress("UNCHECKED_CAST")
        val req: Map<String, Any> =
          jacksonObjectMapper().readValue(body, Map::class.java) as Map<String, Any>

        req["licenseKey"].assert.isEqualTo("mock-mock")
        req["seats"].assert.isEqualTo(1)
        req["keys"].assert.isEqualTo(1)

        val subscription = getSubscription()
        subscription.assert.isNotNull
        subscription!!.includedKeys.assert.isEqualTo(10)
        subscription.includedSeats.assert.isEqualTo(10)
        subscription.seatsLimit.assert.isEqualTo(10)
        subscription.includedKeys.assert.isEqualTo(10)
        subscription.keysLimit.assert.isEqualTo(10)
        subscription.isPayAsYouGo.assert.isEqualTo(false)
      }
    }
  }

  private fun getSubscription() = eeSubscriptionService.findSubscriptionEntity()

  @Test
  fun `set license key operation is not sensitive for non-breaking API change`() {
    eeLicensingMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/set-key") }
      }

      thenAnswer { getSetKeyRequestMapWithAdditionalUnrecognizedFields() }

      verify {
        performAuthPut("/v2/ee-license/set-license-key", mapOf("licenseKey" to "mock-mock")).andIsOk
        getSubscription().assert.isNotNull
      }
    }
  }

  @Test
  fun `prepare operation works fine`() {
    eeLicensingMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/prepare-set-key") }
      }

      thenAnswer { getPrepareRequestMapWithAdditionalUnrecognizedFields() }

      verify {
        performAuthPost(
          "/v2/ee-license/prepare-set-license-key",
          mapOf("licenseKey" to "mock-mock"),
        ).andIsOk.andPrettyPrint.andAssertThatJson {
          node("plan") {
            node("id").isNumber
          }
        }

        captor.allValues.assert.hasSize(1)
      }
    }
  }

  @Test
  fun `refreshes subscription`() {
    prepareSubscription()

    eeLicensingMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/subscription") }
      }

      thenAnswer {
        eeLicensingMockRequestUtil.mockedSubscriptionResponse
      }

      verify {
        performAuthPut(
          "/v2/ee-license/refresh",
          null,
        ).andIsOk

        eeSubscriptionRepository
          .findAll()
          .single()
          .status.assert
          .isEqualTo(SubscriptionStatus.ACTIVE)
        captor.allValues.assert.hasSize(1)
      }
    }
  }

  @Test
  fun `returns info`() {
    prepareSubscription()
    performAuthGet(
      "/v2/ee-license/info",
    ).andIsOk
  }

  @Test
  fun `releases license key info`() {
    prepareSubscription()

    eeLicensingMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/release-key") }
      }

      thenAnswer {
      }

      verify {
        performAuthPut(
          "/v2/ee-license/release-license-key",
          null,
        ).andIsOk

        eeSubscriptionRepository.findAll().assert.isEmpty()
        val body = captor.allValues.single().body as String
        val data = jacksonObjectMapper().readValue(body, Map::class.java)
        data["licenseKey"].assert.isEqualTo("mock")
      }
    }
  }

  private fun prepareSubscription() {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ERROR
        currentPeriodEnd = Date()
        enabledFeatures = Feature.values()
        lastValidCheck = Date()
      },
    )
  }

  private fun getPrepareRequestMapWithAdditionalUnrecognizedFields(): MutableMap<String, Any?> {
    // converting the requestBody to map and adding some unrecognized fields
    val requestMap = eeLicensingMockRequestUtil.mockedPrepareResponse.toJsonMap()
    requestMap["undefinedField"] = "undefined"
    requestMap["undefinedField2"] = 20
    requestMap["undefinedField3"] = false
    return requestMap
  }

  private fun getSetKeyRequestMapWithAdditionalUnrecognizedFields(): MutableMap<String, Any?> {
    // converting the requestBody to map and adding some unrecognized fields
    val requestMap = eeLicensingMockRequestUtil.mockedSubscriptionResponse.toJsonMap()
    requestMap["undefinedField"] = "undefined"
    requestMap["undefinedField2"] = 20
    requestMap["undefinedField3"] = false
    @Suppress("UNCHECKED_CAST")
    val planMap = requestMap["plan"] as MutableMap<String, Any?>
    planMap["undefinedField"] = "undefined"
    planMap["undefinedField2"] = 20
    planMap["undefinedField3"] = false
    return requestMap
  }

  private fun Any.toJsonMap(): MutableMap<String, Any?> {
    val json = jacksonObjectMapper().writeValueAsString(this)
    @Suppress("UNCHECKED_CAST")
    return jacksonObjectMapper().readValue(json, MutableMap::class.java) as MutableMap<String, Any?>
  }

  private fun Map<String, Any?>.toJson(): String {
    return jacksonObjectMapper().writeValueAsString(this)
  }
}
