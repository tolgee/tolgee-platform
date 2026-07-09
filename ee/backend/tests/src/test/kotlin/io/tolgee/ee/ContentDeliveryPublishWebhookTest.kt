package io.tolgee.ee

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.component.automations.processors.ContentDeliveryPublishWebhookData
import io.tolgee.component.automations.processors.WebhookEventType
import io.tolgee.development.testDataBuilder.data.WebhooksTestData
import io.tolgee.ee.service.WebhookConfigService
import io.tolgee.events.OnContentDeliveryPublished
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

@SpringBootTest
class ContentDeliveryPublishWebhookTest : ProjectAuthControllerTest("/v2/projects/") {
  @Autowired
  lateinit var webhookConfigService: WebhookConfigService

  @Autowired
  lateinit var eventPublisher: ApplicationEventPublisher

  @MockitoBean
  @Autowired
  lateinit var restTemplate: RestTemplate

  @MockitoBean
  @Autowired
  @Qualifier("webhookRestTemplate")
  lateinit var webhookRestTemplate: RestTemplate

  lateinit var testData: WebhooksTestData

  @BeforeEach
  fun before() {
    testData = WebhooksTestData()
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
    Mockito.reset(restTemplate, webhookRestTemplate)
    mockWebhookResponse(HttpStatus.OK)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `dispatches CONTENT_DELIVERY_PUBLISH webhook with payload`() {
    testData.webhookConfig.self.eventTypes = mutableSetOf(WebhookEventType.CONTENT_DELIVERY_PUBLISH)
    testDataService.saveTestData(testData.root)

    val invocationsBefore = getWebhookRestTemplateInvocationCount()

    eventPublisher.publishEvent(
      OnContentDeliveryPublished(
        this,
        ContentDeliveryPublishWebhookData(
          projectId = testData.projectBuilder.self.id,
          id = 999L,
          name = "Production CDN",
          slug = "abc123",
          lastPublished = 1718539200000,
          files = listOf("en.json", "de.json"),
        ),
      ),
    )

    waitForNotThrowing(timeout = 10000) {
      getWebhookRestTemplateInvocationCount().assert.isEqualTo(invocationsBefore + 1)
      val callArguments =
        Mockito
          .mockingDetails(webhookRestTemplate)
          .invocations
          .last()
          .arguments
      val httpEntity = callArguments[2] as HttpEntity<String>
      assertThatJson(httpEntity.body!!) {
        node("eventType").isEqualTo("CONTENT_DELIVERY_PUBLISH")
        node("projectId").isEqualTo(testData.projectBuilder.self.id)
        node("contentDeliveryConfig.slug").isEqualTo("abc123")
        node("contentDeliveryConfig.files").isArray.containsExactlyInAnyOrder("en.json", "de.json")
        node("activityData").isNull()
      }
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `does not dispatch to webhooks subscribed to a different event type`() {
    // Webhook is subscribed to PROJECT_ACTIVITY, not CONTENT_DELIVERY_PUBLISH
    testData.webhookConfig.self.eventTypes = mutableSetOf(WebhookEventType.PROJECT_ACTIVITY)
    testDataService.saveTestData(testData.root)

    val invocationsBefore = getWebhookRestTemplateInvocationCount()

    eventPublisher.publishEvent(
      OnContentDeliveryPublished(
        this,
        ContentDeliveryPublishWebhookData(
          projectId = testData.projectBuilder.self.id,
          id = 1L,
          name = "CDN",
          slug = "xyz",
          lastPublished = null,
          files = listOf(),
        ),
      ),
    )

    Thread.sleep(2500)
    getWebhookRestTemplateInvocationCount().assert.isEqualTo(invocationsBefore)

    // Confirm the count stays unchanged — no delayed dispatch
    Thread.sleep(500)
    getWebhookRestTemplateInvocationCount().assert.isEqualTo(invocationsBefore)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `records failure streak when consumer returns 500`() {
    testData.webhookConfig.self.eventTypes = mutableSetOf(WebhookEventType.CONTENT_DELIVERY_PUBLISH)
    testDataService.saveTestData(testData.root)

    mockWebhookResponse(HttpStatus.INTERNAL_SERVER_ERROR)

    eventPublisher.publishEvent(
      OnContentDeliveryPublished(
        this,
        ContentDeliveryPublishWebhookData(
          projectId = testData.projectBuilder.self.id,
          id = 1L,
          name = "CDN",
          slug = "xyz",
          lastPublished = null,
          files = listOf(),
        ),
      ),
    )

    waitForNotThrowing(timeout = 10000) {
      webhookConfigService
        .get(testData.webhookConfig.self.id)
        .firstFailed.assert
        .isNotNull()
    }
  }

  private fun mockWebhookResponse(httpStatus: HttpStatus) {
    doAnswer {
      ResponseEntity.status(httpStatus).build<Any>()
    }.whenever(webhookRestTemplate)
      .exchange(
        any<String>(),
        any<HttpMethod>(),
        any<HttpEntity<*>>(),
        any<Class<*>>(),
      )
  }

  private fun getWebhookRestTemplateInvocationCount() = Mockito.mockingDetails(webhookRestTemplate).invocations.count()
}
