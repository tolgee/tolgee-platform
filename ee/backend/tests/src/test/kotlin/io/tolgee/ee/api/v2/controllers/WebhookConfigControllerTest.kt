package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.ee.service.WebhookConfigService
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Consumer

class WebhookConfigControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: WebhooksTestData

  @Autowired
  lateinit var webhookConfigService: WebhookConfigService

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = WebhooksTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.WEBHOOKS)
  }

  @AfterEach
  fun after() {
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates webhook config`() {
    createWebhook().andAssertThatJson {
      node("id").isValidId.satisfies(
        Consumer {
          it.toLong().let { id ->
            webhookConfigService.find(id).assert.isNotNull()
            entityManager
              .createQuery("""from AutomationAction aa where aa.webhookConfig.id = :id""")
              .setParameter("id", id)
              .resultList
              .assert
              .isNotEmpty()
          }
        },
      )
      node("url").isEqualTo("https://hello.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `doesnt create multiple webhooks without feature enabled`() {
    enabledFeaturesProvider.forceEnabled = setOf()
    createWebhook()
    createWebhook().andIsBadRequest
    enabledFeaturesProvider.forceEnabled = setOf(Feature.WEBHOOKS)
    createWebhook().andIsOk
  }

  private fun createWebhook(): ResultActions {
    return performProjectAuthPost(
      "webhook-configs",
      mapOf("url" to "https://hello.com"),
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates webhook config`() {
    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to "https://hello.com"),
    ).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("url").isEqualTo("https://hello.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes webhook config`() {
    performProjectAuthDelete(
      "webhook-configs/${testData.webhookConfig.self.id}",
    ).andIsOk

    webhookConfigService.find(testData.webhookConfig.self.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists webhook configs`() {
    performProjectAuthGet(
      "webhook-configs",
    ).andAssertThatJson {
      node("_embedded.webhookConfigs") {
        isArray.hasSize(1)
        node("[0].id").isValidId
        node("[0].url").isEqualTo("https://this-will-hopefully-never-exist.com/wh")
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get single webhook config`() {
    performProjectAuthGet(
      "webhook-configs/${testData.webhookConfig.self.id}",
    ).andAssertThatJson {
      node("id").isValidId
      node("url").isEqualTo("https://this-will-hopefully-never-exist.com/wh")
      node("webhookSecret").isString
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `tests a config`() {
    performProjectAuthPost(
      "webhook-configs/${testData.webhookConfig.self.id}/test",
      null,
    ).andIsOk.andAssertThatJson {
      node("success").isBoolean.isFalse
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns enabled field in response`() {
    performProjectAuthGet(
      "webhook-configs/${testData.webhookConfig.self.id}",
    ).andIsOk.andAssertThatJson {
      node("enabled").isEqualTo(true)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates enabled field`() {
    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to testData.webhookConfig.self.url, "enabled" to false),
    ).andIsOk.andAssertThatJson {
      node("enabled").isEqualTo(false)
    }

    webhookConfigService
      .get(testData.webhookConfig.self.id)
      .enabled.assert.isFalse
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `re-enabling clears firstFailed`() {
    executeInNewTransaction {
      val config = webhookConfigService.get(testData.webhookConfig.self.id)
      config.firstFailed = java.util.Date()
      config.enabled = false
      entityManager.merge(config)
    }

    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to testData.webhookConfig.self.url, "enabled" to true),
    ).andIsOk.andAssertThatJson {
      node("enabled").isEqualTo(true)
      node("firstFailed").isNull()
    }

    val updated = webhookConfigService.get(testData.webhookConfig.self.id)
    updated.firstFailed.assert.isNull()
    updated.enabled.assert.isTrue
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `update without enabled field does not change enabled state`() {
    executeInNewTransaction {
      val config = webhookConfigService.get(testData.webhookConfig.self.id)
      config.enabled = false
      entityManager.merge(config)
    }

    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to "https://new-url.com"),
    ).andIsOk.andAssertThatJson {
      node("enabled").isEqualTo(false)
      node("url").isEqualTo("https://new-url.com")
    }
  }
}
