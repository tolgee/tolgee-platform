package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.service.webhooks.WebhookConfigService
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WebhooksControllerTest : ProjectAuthControllerTest("/v2/projects/") {

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
    enabledFeaturesProvider.forceEnabled = listOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES)
  }

  @AfterEach
  fun after() {
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `creates webhook config`() {
    performProjectAuthPost(
      "webhook-configs",
      mapOf("url" to "https://hello.com")
    ).andAssertThatJson {
      node("id").isValidId
      node("url").isEqualTo("https://hello.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `updates webhook config`() {
    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to "https://hello.com")
    ).andIsOk.andAssertThatJson {
      node("id").isValidId
      node("url").isEqualTo("https://hello.com")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `deletes webhook config`() {
    performProjectAuthDelete(
      "webhook-configs/${testData.webhookConfig.self.id}"
    ).andIsOk

    webhookConfigService.find(testData.webhookConfig.self.id).assert.isNull()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `lists webhook configs`() {
    performProjectAuthGet(
      "webhook-configs"
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
      "webhook-configs/${testData.webhookConfig.self.id}"
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
      null
    ).andIsOk.andAssertThatJson {
      node("success").isBoolean.isFalse
    }
  }
}
