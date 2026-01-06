package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.limits.PlanLimitExceededKeysException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededKeysException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate
import java.util.Date

@SpringBootTest
class KeyCountLimitTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  @MockitoBean
  private lateinit var restTemplate: RestTemplate

  @BeforeEach
  fun initMocks() {
    val mockAny = mock<Any>()
    val mockResp = mock<ResponseEntity<Any>>()
    whenever(restTemplate.exchange(any<String>(), any(), any(), any<Class<Any>>())).thenReturn(mockResp)
    whenever(mockResp.body).thenReturn(mockAny)
  }

  @Test
  fun `throws when over the limit`() {
    saveSubscription {
      includedKeys = 1
    }
    val testData = saveTestData(1)
    assertThrows<PlanLimitExceededKeysException> {
      createKey(testData)
    }
  }

  @Test
  fun `does not throw when unlimited (-1)`() {
    saveSubscription {
      includedKeys = -1
      keysLimit = -1
    }
    val testData = saveTestData(1)
    createKey(testData)
  }

  @Test
  fun `does not throw when removing keys`() {
    saveSubscription {
      includedKeys = 2
      keysLimit = 2
    }
    val testData = saveTestData(2)

    saveSubscription {
      includedKeys = 0
      keysLimit = 0
    }
    val keyToDelete =
      testData.projectBuilder.data.keys
        .first()
        .self
    keyService.delete(keyToDelete.id)
  }

  @Test
  fun `does not throw when pay as you go`() {
    saveSubscription {
      this.isPayAsYouGo = true
      this.keysLimit = 10
      this.includedKeys = 1
    }
    val testData = saveTestData(1)
    createKey(testData)
  }

  @Test
  fun `throws when pay as you go and over the limit`() {
    saveSubscription {
      this.isPayAsYouGo = true
      this.includedKeys = 0
      this.keysLimit = 1
    }
    val testData = saveTestData(1)
    assertThrows<PlanSpendingLimitExceededKeysException> {
      createKey(testData)
    }
  }

  // TODO: Test cannot create key when status subscription status is ERROR

  private fun createKey(testData: BaseTestData) {
    keyService.create(testData.project, CreateKeyDto(name = "Over limit key"))
  }

  private fun saveSubscription(build: EeSubscription.() -> Unit = {}) {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ACTIVE
        currentPeriodEnd = Date()
        enabledFeatures = Feature.entries.toTypedArray()
        lastValidCheck = Date()
        this.includedKeys = 1
        this.includedSeats = 1
        this.keysLimit = 1
        this.seatsLimit = 1
        isPayAsYouGo = false
        build(this)
      },
    )
  }

  private fun saveTestData(keyCount: Long = 1): BaseTestData {
    val testData = BaseTestData()
    (1..keyCount).forEach {
      testData.projectBuilder.addKey("$it")
    }
    testDataService.saveTestData(testData.root)
    return testData
  }
}
