package io.tolgee.ee.api.v2.controllers

import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.EeLicensingMockRequestUtil
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.hateoas.ee.uasge.current.CurrentUsageItemModel
import io.tolgee.hateoas.ee.uasge.current.CurrentUsageModel
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.util.Date

class EeSubscriptionUsageControllerTest : AuthorizedControllerTest() {
  @Autowired
  @MockitoBean
  lateinit var restTemplate: RestTemplate

  lateinit var testData: BaseTestData

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
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Test
  fun `it returns correct usage`() {
    prepareSubscription()
    eeLicensingMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/current-subscription-usage") }
      }

      thenAnswer {
        CurrentUsageModel(
          seats = CurrentUsageItemModel(current = 0, included = 10, limit = 10),
          keys = CurrentUsageItemModel(current = 0, included = 5000, limit = 5000),
          strings = CurrentUsageItemModel(current = 0, included = -1, limit = -1),
          credits = CurrentUsageItemModel(current = 10000, included = 10000, limit = 10000),
          isPayAsYouGo = true,
        )
      }

      verify {
        performAuthGet("/v2/ee-current-subscription-usage")
          .andIsOk
          .andAssertThatJson {
            node("seats") {
              node("current").isEqualTo(0)
              node("included").isEqualTo(10)
              node("limit").isEqualTo(10)
            }
            node("strings") {
              node("current").isEqualTo(0)
              node("included").isEqualTo(-1)
              node("limit").isEqualTo(-1)
            }
            node("credits") {
              node("current").isEqualTo(10000)
              node("included").isEqualTo(10000)
              node("limit").isEqualTo(10000)
            }
            node("keys") {
              node("current").isEqualTo(0)
              node("included").isEqualTo(5000)
              node("limit").isEqualTo(5000)
            }
            node("isPayAsYouGo").isEqualTo(true)
          }

        val body =
          this.captor.allValues
            .single()
            .body!!
        assertThatJson(body) {
          node("licenseKey").isEqualTo("mock")
        }
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
}
