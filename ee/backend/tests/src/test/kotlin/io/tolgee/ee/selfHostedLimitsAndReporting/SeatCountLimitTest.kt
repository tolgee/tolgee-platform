package io.tolgee.ee.selfHostedLimitsAndReporting

import io.tolgee.AbstractSpringTest
import io.tolgee.api.SubscriptionStatus
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.repository.EeSubscriptionRepository
import io.tolgee.exceptions.limits.PlanLimitExceededSeatsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededSeatsException
import io.tolgee.model.UserAccount
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.Date

@SpringBootTest()
class SeatCountLimitTest : AbstractSpringTest() {
  @Autowired
  private lateinit var eeSubscriptionRepository: EeSubscriptionRepository

  @Test
  fun `throws when over the limit`() {
    saveSubscription {
      includedSeats = 1
      seatsLimit = 1
    }
    saveTestData(1)
    assertThrows<PlanLimitExceededSeatsException> {
      createUser()
    }
  }

  @Test
  fun `does not throw when unlimited (-1)`() {
    saveSubscription {
      includedSeats = -1
      seatsLimit = -1
    }
    saveTestData(1)
    createUser()
  }

  @Test
  fun `does not throw when pay as you go`() {
    saveSubscription {
      this.isPayAsYouGo = true
      this.includedSeats = 0
      this.seatsLimit = 10
    }
    saveTestData(1)
    createUser()
  }

  @Test
  fun `throws when pay as you go and over the limit`() {
    saveSubscription {
      this.isPayAsYouGo = true
      this.includedSeats = 1
      this.seatsLimit = 1
    }
    saveTestData(1)
    assertThrows<PlanSpendingLimitExceededSeatsException> {
      createUser()
    }
  }

  @Test
  fun `does not throw when removing users`() {
    saveSubscription {
      includedSeats = 2
      seatsLimit = 2
    }

    val testData = saveTestData(2)

    saveSubscription {
      includedSeats = 0
      seatsLimit = 0
    }
    val userToDelete =
      testData.root.data.userAccounts
        .first()
        .self.id
    userAccountService.delete(userToDelete)
  }

  private fun createUser() {
    userAccountService.createUser(
      UserAccount(
        name = "Over the limit",
        username = "overthelimit@ttt.tt",
      ),
    )
  }

  private fun saveSubscription(build: EeSubscription.() -> Unit = {}) {
    eeSubscriptionRepository.save(
      EeSubscription().apply {
        licenseKey = "mock"
        name = "Plaaan"
        status = SubscriptionStatus.ERROR
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

  private fun saveTestData(userCount: Long = 1): BaseTestData {
    val testData = BaseTestData()
    repeat(userCount.toInt()) {
      testData.root.addUserAccount {
        username = "user$it"
      }
    }
    testDataService.saveTestData(testData.root)
    return testData
  }
}
