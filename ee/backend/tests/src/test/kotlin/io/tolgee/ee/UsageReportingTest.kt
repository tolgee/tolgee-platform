package io.tolgee.ee

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
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
  fun `it reports seat usage`() {
    saveSubscription()

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

  @Test
  fun `it reports keys usage`() {
    testWithBaseTestData { testData, captor ->
      // key add & delete
      val key = keyService.create(testData.project, "key1", null)
      captor.assertKeys(1)
      keyService.delete(key.id)
      captor.assertKeys(0)
    }
  }

  @Test
  fun `it does not execute many requests`() {
    testWithBaseTestData { testData, captor ->
      // create 10 keys
      executeInNewTransaction {
        (1..10).forEach {
          keyService.create(testData.project, "key$it", null)
        }
      }

      // now we have reported 10 keys
      captor.assertKeys(10)

      // check it doesn't do request for every key
      captor.allValues.assert.hasSizeLessThan(10)
    }
  }

  @Test
  fun `it reports key usage when project is deleted`() {
    testWithBaseTestData { testData, captor ->
      keyService.create(testData.project, "key1", null)
      // delete the project
      projectService.deleteProject(testData.project.id)
      captor.assertKeys(0)
    }
  }

  @Test
  fun `it reports usage when organization is deleted`() {
    testWithBaseTestData { testData, captor ->
      keyService.create(testData.project, "key1", null)
      // delete the organization
      organizationService.delete(testData.projectBuilder.self.organizationOwner)
      captor.assertKeys(0)
    }
  }

  private fun testWithBaseTestData(test: (BaseTestData, KArgumentCaptor<HttpEntity<*>>) -> Unit) {
    saveSubscription()
    val testData = BaseTestData()
    testDataService.saveTestData(testData.root)

    eeLicenseMockRequestUtil.mock {
      whenReq {
        this.method = { it == HttpMethod.POST }
        this.url = { it.contains("/v2/public/licensing/report-usage") }
      }

      thenAnswer {
      }

      verify {
        test(testData, captor)
      }
    }
  }

  private fun saveSubscription() {
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
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertSeats(seats: Long) {
    val data = parseRequestArgs()
    data["seats"].toString().assert.isEqualTo(seats.toString())
  }

  fun KArgumentCaptor<HttpEntity<*>>.assertKeys(keys: Long) {
    val data = parseRequestArgs()
    data["keys"].toString().assert.isEqualTo(keys.toString())
  }

  private fun KArgumentCaptor<HttpEntity<*>>.parseRequestArgs(): Map<*, *> =
    objectMapper.readValue(this.lastValue.body as String, Map::class.java)
}
