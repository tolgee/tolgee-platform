package io.tolgee.ee

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.component.fileStorage.S3FileStorage
import io.tolgee.component.fileStorage.S3FileStorageFactory
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.contentDelivery.S3ContentStorageConfig
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.client.RestTemplate
import java.net.URI

@SpringBootTest
class ContentDeliveryPublishWebhookTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Autowired
  @Qualifier("webhookRestTemplate")
  lateinit var webhookRestTemplate: RestTemplate

  @Autowired
  @MockitoSpyBean
  private lateinit var s3FileStorageFactory: S3FileStorageFactory

  @Autowired
  private lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  lateinit var testData: WebhooksTestData
  private var contentDeliveryConfigId: Long = 0
  private var storageId: Long = 0

  @BeforeEach
  fun before() {
    Mockito.reset(webhookRestTemplate, s3FileStorageFactory)
    doAnswer { mock<S3FileStorage>() }.whenever(s3FileStorageFactory).create(any())
    doAnswer { ResponseEntity.status(HttpStatus.OK).build<Any>() }
      .whenever(webhookRestTemplate)
      .exchange(any<URI>(), any<HttpMethod>(), any<HttpEntity<*>>(), any<Class<*>>())
  }

  @AfterEach
  fun after() {
    enabledFeaturesProvider.forceEnabled = null
    batchJobConcurrentLauncher.pause = false
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  @Suppress("UNCHECKED_CAST")
  fun `sends webhook when content delivery config is published`() {
    subscribeWebhookTo(AutomationTriggerType.CONTENT_DELIVERY_PUBLISH)
    val invocationsBefore = webhookInvocationCount()

    publish()

    waitForNotThrowing(timeout = 10000) {
      webhookInvocationCount().assert.isEqualTo(invocationsBefore + 1)
      val httpEntity =
        Mockito
          .mockingDetails(webhookRestTemplate)
          .invocations
          .last()
          .arguments[2] as HttpEntity<String>
      assertThatJson(httpEntity.body!!) {
        node("eventType").isEqualTo("CONTENT_DELIVERY_PUBLISH")
        node("projectId").isEqualTo(testData.projectBuilder.self.id)
        node("activityData").isNull()
        node("contentDeliveryConfig.id").isEqualTo(contentDeliveryConfigId)
        node("contentDeliveryConfig.name").isEqualTo("Production CDN")
        node("contentDeliveryConfig.slug").isString
        node("contentDeliveryConfig.lastPublished").isNumber
        node("contentDeliveryConfig.files").isArray.isNotEmpty
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not send webhook when subscribed to project activity only`() {
    subscribeWebhookTo(AutomationTriggerType.ACTIVITY)
    val invocationsBefore = webhookInvocationCount()

    publish()

    Thread.sleep(2000)
    webhookInvocationCount().assert.isEqualTo(invocationsBefore)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends one webhook per publish when subscribed to both event types`() {
    subscribeWebhookTo(AutomationTriggerType.ACTIVITY, AutomationTriggerType.CONTENT_DELIVERY_PUBLISH)
    enabledFeaturesProvider.forceEnabled =
      setOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES, Feature.MULTIPLE_CONTENT_DELIVERY_CONFIGS, Feature.WEBHOOKS)
    performProjectAuthPost(
      "content-delivery-configs",
      mapOf("name" to "Created via API", "contentStorageId" to storageId),
    ).andIsOk.andAssertThatJson { node("id").isNumber.satisfies({ contentDeliveryConfigId = it.toLong() }) }
    Thread.sleep(1500)
    val invocationsBefore = webhookInvocationCount()

    publish()

    waitForNotThrowing(timeout = 10000) {
      webhookInvocationCount().assert.isEqualTo(invocationsBefore + 1)
    }
    Thread.sleep(1500)
    webhookInvocationCount().assert.isEqualTo(invocationsBefore + 1)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `sends a webhook for every publish`() {
    subscribeWebhookTo(AutomationTriggerType.CONTENT_DELIVERY_PUBLISH)
    val invocationsBefore = webhookInvocationCount()

    batchJobConcurrentLauncher.pause = true
    publish()
    publish()
    pendingAutomationJobCount().assert.isEqualTo(2)
    batchJobConcurrentLauncher.pause = false

    waitForNotThrowing(timeout = 10000) {
      webhookInvocationCount().assert.isEqualTo(invocationsBefore + 2)
    }
  }

  private fun pendingAutomationJobCount(): Long =
    entityManager
      .createQuery(
        "select count(j) from BatchJob j where j.project.id = :projectId and j.type = 'AUTOMATION' and j.status = 'PENDING'",
        Long::class.javaObjectType,
      ).setParameter("projectId", testData.projectBuilder.self.id)
      .singleResult

  @Test
  @ProjectJWTAuthTestMethod
  fun `reports the publish it was queued for, not the config state at delivery time`() {
    subscribeWebhookTo(AutomationTriggerType.CONTENT_DELIVERY_PUBLISH)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.PROJECT_LEVEL_CONTENT_STORAGES, Feature.WEBHOOKS)
    val invocationsBefore = webhookInvocationCount()

    batchJobConcurrentLauncher.pause = true
    publish()
    performProjectAuthPut(
      "content-delivery-configs/$contentDeliveryConfigId",
      mapOf("name" to "Renamed after publish", "contentStorageId" to storageId),
    ).andIsOk
    batchJobConcurrentLauncher.pause = false

    waitForNotThrowing(timeout = 10000) {
      webhookInvocationCount().assert.isEqualTo(invocationsBefore + 1)
      @Suppress("UNCHECKED_CAST")
      val httpEntity =
        Mockito
          .mockingDetails(webhookRestTemplate)
          .invocations
          .last()
          .arguments[2] as HttpEntity<String>
      assertThatJson(httpEntity.body!!) {
        node("contentDeliveryConfig.name").isEqualTo("Production CDN")
      }
    }
  }

  private fun publish() {
    performProjectAuthPost("content-delivery-configs/$contentDeliveryConfigId").andIsOk
  }

  private fun subscribeWebhookTo(vararg triggerTypes: AutomationTriggerType) {
    testData = WebhooksTestData()
    val automation = testData.automation.self
    automation.triggers.single().type = triggerTypes.first()
    triggerTypes.drop(1).forEach { type ->
      automation.triggers.add(AutomationTrigger(automation).apply { this.type = type })
    }
    testData.projectBuilder.addKey("key") { addTranslation("en", "Hello") }
    val storage =
      testData.projectBuilder.addContentStorage {
        s3ContentStorageConfig =
          S3ContentStorageConfig(this).apply {
            bucketName = "fake"
            accessKey = "fake"
            secretKey = "fake"
            endpoint = "fake"
            signingRegion = "fake"
          }
      }
    val contentDeliveryConfig =
      testData.projectBuilder.addContentDeliveryConfig {
        name = "Production CDN"
        contentStorage = storage.self
        filterState = listOf(TranslationState.TRANSLATED, TranslationState.REVIEWED)
      }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    contentDeliveryConfigId = contentDeliveryConfig.self.id
    storageId = storage.self.id
  }

  private fun webhookInvocationCount() = Mockito.mockingDetails(webhookRestTemplate).invocations.count()
}
