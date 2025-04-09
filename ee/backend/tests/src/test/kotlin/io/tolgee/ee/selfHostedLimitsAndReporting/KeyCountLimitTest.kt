package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.limits.PlanKeysLimitExceeded
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.web.client.RestTemplate
import java.util.*

@SpringBootTest()
class KeyCountLimitTest : AbstractSpringTest() {

  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Autowired
  @MockBean
  private lateinit var restTemplate: RestTemplate

  @Test
  fun `throws when over the limit`() {
    saveSubscription {
      includedKeys = 1
    }
    val testData = saveTestData(1)
    assertThrows<PlanKeysLimitExceeded> {
      createKey(testData)
    }
  }

  @Test
  fun `does not throw when unlimited (-1)`() {
    saveSubscription{
      includedKeys = -1
    }
    val testData = saveTestData(1)
    createKey(testData)
  }

  @Test
  fun `does not throw when pay as you go`() {
    saveSubscription {
      this.isPayAsYouGo = true
    }
    val testData = saveTestData(1)
    createKey(testData)
  }

  private fun createKey(testData: BaseTestData) {
    keyService.create(testData.project, CreateKeyDto(name = "Over limit key"))
  }

  private fun saveSubscription(build: EeSubscription.() -> Unit = {}) {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ERROR
        currentPeriodEnd = Date()
        cancelAtPeriodEnd = false
        enabledFeatures = Feature.entries.toTypedArray()
        lastValidCheck = Date()
        this.includedKeys = 1
        this.includedSeats = 1
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
