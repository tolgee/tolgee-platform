package io.tolgee.ee.component

import io.tolgee.AbstractSpringTest
import io.tolgee.component.automations.processors.WebhookAutoDisableChecker
import io.tolgee.config.TestEmailConfiguration
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.service.WebhookConfigService
import io.tolgee.fixtures.EmailTestUtil
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.assert
import io.tolgee.util.addDays
import io.tolgee.util.addMinutes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestEmailConfiguration::class)
class WebhookAutoDisableCheckerTest : AbstractSpringTest() {
  @Autowired
  lateinit var webhookAutoDisableChecker: WebhookAutoDisableChecker

  @Autowired
  lateinit var webhookConfigService: WebhookConfigService

  @Autowired
  lateinit var emailTestUtil: EmailTestUtil

  lateinit var testData: WebhooksTestData

  @BeforeEach
  fun setup() {
    emailTestUtil.initMocks()
    testData = WebhooksTestData()
    currentDateProvider.forcedDate = currentDateProvider.date
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
  }

  @Test
  fun `disables webhook failing for more than 3 days`() {
    testData.webhookConfig.self.firstFailed = currentDateProvider.date.addDays(-4)
    testData.webhookConfig.self.enabled = true
    testDataService.saveTestData(testData.root)

    val result = webhookAutoDisableChecker.checkAfterFailure(testData.webhookConfig.self)
    result.assert.isTrue

    val config = webhookConfigService.get(testData.webhookConfig.self.id)
    config.enabled.assert.isFalse
    config.autoDisabled.assert.isTrue
  }

  @Test
  fun `does not disable webhook failing for less than 3 days`() {
    testData.webhookConfig.self.firstFailed = currentDateProvider.date.addDays(-2)
    testData.webhookConfig.self.enabled = true
    testDataService.saveTestData(testData.root)

    val result = webhookAutoDisableChecker.checkAfterFailure(testData.webhookConfig.self)
    result.assert.isFalse

    val config = webhookConfigService.get(testData.webhookConfig.self.id)
    config.enabled.assert.isTrue
  }

  @Test
  fun `sends warning after 6 hours of failure`() {
    testData.webhookConfig.self.firstFailed = currentDateProvider.date.addMinutes(-420)
    testData.webhookConfig.self.enabled = true
    testData.webhookConfig.self.autoDisableNotified = false
    testDataService.saveTestData(testData.root)

    val result = webhookAutoDisableChecker.checkAfterFailure(testData.webhookConfig.self)
    result.assert.isFalse

    val config = webhookConfigService.get(testData.webhookConfig.self.id)
    config.autoDisableNotified.assert.isTrue
    config.enabled.assert.isTrue

    waitForNotThrowing(timeout = 5000) {
      emailTestUtil.verifyEmailSent()
      emailTestUtil.assertEmailTo.isEqualTo(testData.user.username)
    }
  }

  @Test
  fun `does not send warning twice`() {
    testData.webhookConfig.self.firstFailed = currentDateProvider.date.addMinutes(-420)
    testData.webhookConfig.self.enabled = true
    testData.webhookConfig.self.autoDisableNotified = true
    testDataService.saveTestData(testData.root)

    webhookAutoDisableChecker.checkAfterFailure(testData.webhookConfig.self)

    Thread.sleep(1000)
    emailTestUtil.messageArgumentCaptor.allValues.assert
      .isEmpty()
  }

  @Test
  fun `sends disable email`() {
    testData.webhookConfig.self.firstFailed = currentDateProvider.date.addDays(-4)
    testData.webhookConfig.self.enabled = true
    testDataService.saveTestData(testData.root)

    webhookAutoDisableChecker.checkAfterFailure(testData.webhookConfig.self)

    waitForNotThrowing(timeout = 5000) {
      emailTestUtil.verifyEmailSent()
      emailTestUtil.assertEmailTo.isEqualTo(testData.user.username)
    }
  }
}
