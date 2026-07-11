package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.configuration.tolgee.WebhookProperties
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class WebhookConfigUrlSecurityTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: WebhooksTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @Autowired
  private lateinit var webhookProperties: WebhookProperties

  @BeforeEach
  fun setup() {
    testData = WebhooksTestData()
    projectSupplier = { testData.projectBuilder.self }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    enabledFeaturesProvider.forceEnabled = setOf(Feature.WEBHOOKS)
    internalProperties.disableUrlSsrfProtection = false
    webhookProperties.allowLocalAddresses = false
  }

  @AfterEach
  fun after() {
    enabledFeaturesProvider.forceEnabled = null
    internalProperties.disableUrlSsrfProtection = true
    webhookProperties.allowLocalAddresses = false
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects local url on create when allowLocalAddresses is false`() {
    performProjectAuthPost(
      "webhook-configs",
      mapOf("url" to "http://localhost"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `allows local url on create when allowLocalAddresses is true`() {
    webhookProperties.allowLocalAddresses = true
    performProjectAuthPost(
      "webhook-configs",
      mapOf("url" to "http://localhost"),
    ).andIsOk.andAssertThatJson {
      node("url").isEqualTo("http://localhost")
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `still rejects non-http scheme on create when allowLocalAddresses is true`() {
    webhookProperties.allowLocalAddresses = true
    performProjectAuthPost(
      "webhook-configs",
      mapOf("url" to "ftp://localhost"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `SSRF protection is active in this context`() {
    performProjectAuthPost(
      "webhook-configs",
      mapOf("url" to "http://127.0.0.1"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `rejects changing url to local on update when allowLocalAddresses is false`() {
    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to "http://localhost"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `allows changing url to local on update when allowLocalAddresses is true`() {
    webhookProperties.allowLocalAddresses = true
    performProjectAuthPut(
      "webhook-configs/${testData.webhookConfig.self.id}",
      mapOf("url" to "http://localhost"),
    ).andIsOk.andAssertThatJson {
      node("url").isEqualTo("http://localhost")
    }
  }
}
